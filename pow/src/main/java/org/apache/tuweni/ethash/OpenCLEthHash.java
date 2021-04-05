/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.ethash;

import com.aparapi.Kernel;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class OpenCLEthHash extends Kernel {

    private final byte[] contentToHash;
    private final long nonceStartRange;
    private final long datasetSize;
    private int[] globalCache;
    private final byte[] target;
    @Local
    private byte[] output = new byte[64];

    private final MessageDigest keccak512Digest;

    private final MessageDigest keccak256Digest;

    public OpenCLEthHash(long epoch, byte[] contentToHash, long nonceStartRange, byte[] target) {
        this.contentToHash = contentToHash;
        this.nonceStartRange = nonceStartRange;
        this.datasetSize = getFullSize(epoch);
        this.target = target;
        try {
            keccak512Digest = MessageDigest.getInstance("KECCAK-512");
            keccak256Digest = MessageDigest.getInstance("KECCAK-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Local
    private byte[] solution = new byte[32];

    @Override
    public void run() {
        int id = getGlobalId();
        long candidateNonce = nonceStartRange + id;
        byte[] result = hashimoto(
                datasetSize,
                contentToHash,
                toLittleEndian(candidateNonce));
        for (int i = 0; i < 32; i++) {
            solution[i] = result[32 + i];
        }
        if (compareLittleEndian(solution, target) < 0) {
            System.out.println("Solution found!");
            for (int i = 0; i < 64; i++) {
                output[i] = result[i];
            }
            cancelMultiPass();
        }
    }

    private int compareLittleEndian(byte[] a, byte[] b) {
        int lengthComparison = Integer.compare(a.length, b.length);
        if (lengthComparison != 0) {
            return lengthComparison;
        }
        for (int i = 31; i >= 0; i--) {
            int comparison = Integer.compare(a[i] & 0xFF, b[i] & 0xFF);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    public byte[] getOutput() {
        return output;
    }

    /**
     * byte[] in word.
     */
    public int WORD_BYTES = 4;
    /**
     * bytes in dataset at genesis
     */
    public long DATASET_BYTES_INIT = (long) Math.pow(2, 30);
    /**
     * dataset growth per epoch
     */
    public long DATASET_BYTES_GROWTH = (long) Math.pow(2, 23);
    /**
     * bytes in cache at genesis
     */
    public long CACHE_BYTES_INIT = (long) Math.pow(2, 24);

    /**
     * cache growth per epoch
     */
    public long CACHE_BYTES_GROWTH = (long) Math.pow(2, 17);
    /**
     * Size of the DAG relative to the cache
     */
    public int CACHE_MULTIPLIER = 1024;
    /**
     * blocks per epoch
     */
    public int EPOCH_LENGTH = 30000;
    /**
     * width of mix
     */
    public int MIX_BYTES = 128;

    /**
     * hash length in bytes
     */
    public int HASH_BYTES = 64;
    /**
     * Number of words in a hash
     */
    private int HASH_WORDS = HASH_BYTES / WORD_BYTES;
    /**
     * number of parents of each dataset element
     */
    public int DATASET_PARENTS = 256;
    /**
     * number of rounds in cache production
     */
    public int CACHE_ROUNDS = 3;
    /**
     * number of accesses in hashimoto loop
     */
    public int ACCESSES = 64;

    public int FNV_PRIME = 0x01000193;


    /**
     * Hashimoto Light Hashing.
     *
     * @param size   the size of the full data set
     * @param cache  EthHash Cache
     * @param header Truncated BlockHeader hash
     * @param nonce  Nonce to use for hashing
     * @return A byte array holding MixHash in its first 32 bytes and the EthHash result in the bytes 32 to 63
     */
    public byte[] hashimotoLight(long size, int[] cache, byte[] header, byte[] nonce) {
        return hashimoto(size, header, nonce);
    }

    @Local
    private byte[] mixBuffer = new byte[MIX_BYTES];
    @Local
    private int[] mix = new int[MIX_BYTES / 4];
    @Local
    private int[] mixCache = new int[MIX_BYTES / 4];

    private byte[] hashimoto(long size, byte[] header, byte[] nonce) {

        long n = Long.divideUnsigned(size, MIX_BYTES);
        MessageDigest digest = keccak512Digest;
        digest.update(header);
        digest.update(nonce);
        byte[] seed = digest.digest();
        digest.reset();
        int eltsLength = MIX_BYTES / HASH_BYTES;
        for (int i = 0; i < eltsLength; ++i) {
            for (int j = 0; j < seed.length; j++) {
                mixBuffer[(i * 32) + j] = seed[j];
            }
        }
        for (int i = 0; i < MIX_BYTES / 4; ++i) {
            mix[i] = getInt(mixBuffer, i * 4);
        }

        int firstSeed = mix[0];
        for (int i = 0; i < ACCESSES; ++i) {
            int fnvValue = fnv(firstSeed ^ i, mix[i % (MIX_BYTES / WORD_BYTES)]);
            int p = fnvValue % (int) n;
            for (int j = 0; j < MIX_BYTES / HASH_BYTES; ++j) {
                byte[] lookupResult = calcDatasetItem(globalCache, (p * 2) + j);
                for (int k = 0; k < lookupResult.length / 4; k++) {
                    mixCache[j] = getInt(lookupResult, k * 4);
                }
            }
            for (int k = 0; k < mix.length; k++) {
                mix[k] = fnv(mix[k], mixCache[k]);
            }
        }

        byte[] result = EMPTY;
        for (int i = 0; i < mix.length / 4; i++) {
            int index = i * 4;
            byte[] arr = toLittleEndian(fnv(fnv(fnv(mix[index], mix[index + 1]), mix[index + 2]), mix[index + 3]));
            result[i * 4] = arr[0];
            result[i * 4 + 1] = arr[1];
            result[i * 4 + 2] = arr[2];
            result[i * 4 + 3] = arr[3];
        }
        MessageDigest keccak256 = keccak256Digest;
        keccak256.update(seed);
        keccak256.update(result);
        byte[] resultPart2 = keccak256.digest();

        for (int i = 0; i < result.length; i++) {
            combined[i] = result[i];
        }
        for (int i = 0; i < resultPart2.length; i++) {
            combined[i + result.length] = resultPart2[i];
        }
//        System.arraycopy(result, 0, combined, 0, result.length);
//        System.arraycopy(resultPart2, 0, combined, result.length, resultPart2.length);

        return combined;
    }

    @Local
    private byte[] combined = new byte[64];

    /**
     * Calculates the EthHash Epoch for a given block number.
     *
     * @param block Block Number
     * @return EthHash Epoch
     */
    public long epoch(long block) {
        return block / EPOCH_LENGTH;
    }

    /**
     * Provides the size of the cache at a given block number
     *
     * @param block_number the block number
     * @return the size of the cache at the block number, in bytes
     */
    public int getCacheSize(long block_number) {
        long sz = CACHE_BYTES_INIT + CACHE_BYTES_GROWTH * (block_number / EPOCH_LENGTH);
        sz -= HASH_BYTES;
        while (!isPrime(sz / HASH_BYTES)) {
            sz -= 2 * HASH_BYTES;
        }
        return (int) sz;
    }

    /**
     * Provides the size of the full dataset at a given block number
     *
     * @param block_number the block number
     * @return the size of the full dataset at the block number, in bytes
     */
    public long getFullSize(long block_number) {
        long sz = DATASET_BYTES_INIT + DATASET_BYTES_GROWTH * (block_number / EPOCH_LENGTH);
        sz -= MIX_BYTES;
        while (!isPrime(sz / MIX_BYTES)) {
            sz -= 2 * MIX_BYTES;
        }
        return sz;
    }

    /**
     * Generates the EthHash cache for given parameters.
     *
     * @param cacheSize Size of the cache to generate
     * @param block     Block Number to generate cache for
     */
    public void mkCache(int cacheSize, long block) {
        int rows = cacheSize / HASH_BYTES;
        byte[] completeCache = new byte[cacheSize];
        byte[] blockseed = keccak512Digest.digest(dagSeed(block));
        for (int i = 0; i < blockseed.length; i++) {
            completeCache[i] = blockseed[i];
        }

        for (int i = 1; i < rows; i++) {
            byte[] output = keccak512Digest.digest(blockseed);
            blockseed = output;
            for (int j = 0; j < blockseed.length; j++) {
                completeCache[i * 32 + j] = output[j];
            }
        }


        byte[] temp = new byte[HASH_BYTES];
        for (int i = 0; i < CACHE_ROUNDS; ++i) {
            for (int j = 0; j < rows; ++j) {
                int offset = j * HASH_BYTES;
                for (int k = 0; k < HASH_BYTES; ++k) {
                    temp[k] = (byte) (completeCache[(j - 1 + rows) % rows * HASH_BYTES + k]
                            ^ (completeCache[
                            Integer.remainderUnsigned(getInt(completeCache, offset), rows)
                                    * HASH_BYTES
                                    + k]));
                }
                temp = keccak512Digest.digest(temp);
                for (int z = 0 ; z < HASH_BYTES ; z++) {
                    completeCache[offset + z] = temp[z];
                }
            }
        }
        int[] result = new int[completeCache.length / 4];
        for (int i = 0; i < result.length; i++) {
            result[i] = getInt(completeCache, i * 4);
        }

        this.globalCache = result;
    }

    private int getInt(byte[] bytes, int offset) {
        int value = 0;
        value |= ((int) bytes[offset + 3] & 0xFF) << 24;
        value |= ((int) bytes[offset + 2] & 0xFF) << 16;
        value |= ((int) bytes[offset + 1] & 0xFF) << 8;
        value |= ((int) bytes[offset] & 0xFF);
        return value;
    }

    @Local
    private byte[] littleEndianLong = new byte[8];

    private byte[] toLittleEndian(long value) {
        byte[] res = littleEndianLong;
        res[0] = (byte) ((value) & 0xFF);
        res[1] = (byte) ((value >> 8) & 0xFF);
        res[2] = (byte) ((value >> 16) & 0xFF);
        res[3] = (byte) ((value >> 24) & 0xFF);
        res[4] = (byte) ((value >> 32) & 0xFF);
        res[5] = (byte) ((value >> 40) & 0xFF);
        res[6] = (byte) ((value >> 48) & 0xFF);
        res[7] = (byte) ((value >> 56) & 0xFF);
        return res;
    }

    @Local
    private byte[] littleEndianInt = new byte[4];

    private byte[] toLittleEndian(int value) {
        byte[] res = littleEndianInt;
        res[0] = (byte) ((value) & 0xFF);
        res[1] = (byte) ((value >> 8) & 0xFF);
        res[2] = (byte) ((value >> 16) & 0xFF);
        res[3] = (byte) ((value >> 24) & 0xFF);
        return res;
    }

    @Local
    private int[] mixInts = new int[HASH_BYTES / 4];

    /**
     * Calculate a data set item based on the previous cache for a given index
     *
     * @param cache the DAG cache
     * @param index the current index
     * @return a new DAG item to append to the DAG
     */
    public byte[] calcDatasetItem(int[] cache, int index) {
        int rows = cache.length / HASH_WORDS;
        int offset = Integer.remainderUnsigned(index, rows) * HASH_WORDS;
        mixInts[0] = cache[offset] ^ index;
        for (int i = 0 ; i < HASH_WORDS - 1 ; i++) {
            mixInts[i + 1] = cache[offset + 1 + i];
        }
        //System.arraycopy(cache, offset + 1, mixInts, 1, HASH_WORDS - 1);
        byte[] buffer = intToByte(mixInts);
        buffer = keccak512Digest.digest(buffer);
        keccak512Digest.reset();
        for (int i = 0; i < mixInts.length; i++) {
            mixInts[i] = getInt(buffer, i * 4);
        }
        for (int i = 0; i < DATASET_PARENTS; ++i) {
            fnvHash(
                    mixInts,
                    cache,
                    Integer.remainderUnsigned(fnv(index ^ i, mixInts[i % 16]), rows) * HASH_WORDS);
        }
        keccak512Digest.update(intToByte(mixInts));
        byte[] result = keccak512Digest.digest();
        keccak512Digest.reset();
        return result;
    }

    @Local
    private final byte[] EMPTY = new byte[32];

    private byte[] dagSeed(long block) {
        byte[] seed = EMPTY;
        if (Long.compareUnsigned(block, EPOCH_LENGTH) >= 0) {
            for (int i = 0; i < Long.divideUnsigned(block, EPOCH_LENGTH); i++) {
                seed = keccak256Digest.digest(seed);
            }
        }
        return seed;
    }

    private int fnv(int v1, int v2) {
        return (v1 * FNV_PRIME) ^ v2;
    }

    private void fnvHash(int[] mix, int[] cache, int offset) {
        for (int i = 0; i < mix.length; i++) {
            mix[i] = fnv(mix[i], cache[offset + i]);
        }
    }

    @Local
    private byte[] buffer = new byte[64];

    private byte[] intToByte(int[] ints) {
        byte[] result = buffer;
        for (int i = 0; i < ints.length; i++) {
            byte[] intBytes = toLittleEndian(ints[i]);
            for (int j = 0; j < 4; j++) {
                result[i * 4 + j] = intBytes[j];
            }
        }
        return result;
    }

    private boolean isPrime(long number) {
        if (number < 2) {
            return false;
        }
        int max = (int) Math.sqrt(number);
        for (int i = 2; i < max; i++) {
            if (number % i == 0) {
                return false;
            }
        }
        return true;
    }
}

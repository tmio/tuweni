/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sovereign

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.SECP256K1.KeyPair
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei

// pragma solidity >=0.7.0 <0.9.0;
//
// /**
// * @title Storage
// * @dev Store & retrieve value in a variable
// */
// contract Storage {
//
//    uint256 number;
//
//    /**
//     * @dev Store value in variable
//     * @param num value to store
//     */
//    function store(uint256 num) public {
//        number = num;
//    }
//
//    /**
//     * @dev Return value
//     * @return value of 'number'
//     */
//    function retrieve() public view returns (uint256){
//        return number;
//    }
// }
val contract = Bytes.fromHexString("0x608060405234801561001057600080fd5b50610150806100206000396000f3fe608060405234801561001057600080fd5b50600436106100365760003560e01c80632e64cec11461003b5780636057361d14610059575b600080fd5b610043610075565b60405161005091906100d9565b60405180910390f35b610073600480360381019061006e919061009d565b61007e565b005b60008054905090565b8060008190555050565b60008135905061009781610103565b92915050565b6000602082840312156100b3576100b26100fe565b5b60006100c184828501610088565b91505092915050565b6100d3816100f4565b82525050565b60006020820190506100ee60008301846100ca565b92915050565b6000819050919050565b600080fd5b61010c816100f4565b811461011757600080fd5b5056fea2646970667358221220404e37f487a89a932dca5e77faaf6ca2de3b991f93d230604b1b8daaef64766264736f6c63430008070033")

fun createContractDeployTransaction(keyPair: KeyPair): Transaction {
  return Transaction(UInt256.ONE, Wei.valueOf(1500000), Gas.valueOf(30000000000), null, Wei.valueOf(0), contract, keyPair)
}

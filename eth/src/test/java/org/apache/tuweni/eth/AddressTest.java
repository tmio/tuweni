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
package org.apache.tuweni.eth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.junit.BouncyCastleExtension;
import org.apache.tuweni.units.bigints.UInt256;
import org.apache.tuweni.units.ethereum.Gas;
import org.apache.tuweni.units.ethereum.Wei;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
public class AddressTest {
  /*
  {
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
  	"blockHash": "0xdf10741b3b0820015caba402f3a230237af86ac0302a923bba4d4f81610c13f0",
  	"blockNumber": "0x64b991",
  	"from": "0xc82a6220398714f74e2d929309f4c5b1d4f7b0f6",
  	"gas": "0x1abffe",
  	"gasPrice": "0x4a817c800",
  	"hash": "0x24158c06c57f4a2814b8b31809378c4a457028d57dcf3c29dfbbc13f850ebfea",
  	"input": "0x60806040523480156200001157600080fd5b50336000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055506040805190810160405280600381526020017f4332450000000000000000000000000000000000000000000000000000000000815250600290805190602001906200009f929190620001f8565b506040805190810160405280600981526020017f43324520546f6b656e000000000000000000000000000000000000000000000081525060039080519060200190620000ed929190620001f8565b506012600460006101000a81548160ff021916908360ff1602179055506a52b7d2dcc80cd2e400000060058190555060055460066000732ec73685a63f0d3c21dbf4867c3c22a6f170bc1873ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002081905550732ec73685a63f0d3c21dbf4867c3c22a6f170bc1873ffffffffffffffffffffffffffffffffffffffff16600073ffffffffffffffffffffffffffffffffffffffff167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef6005546040518082815260200191505060405180910390a3620002a7565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f106200023b57805160ff19168380011785556200026c565b828001600101855582156200026c579182015b828111156200026b5782518255916020019190600101906200024e565b5b5090506200027b91906200027f565b5090565b620002a491905b80821115620002a057600081600090555060010162000286565b5090565b90565b61168f80620002b76000396000f300608060405260043610610112576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806306fdde0314610117578063095ea7b3146101a757806318160ddd1461020c57806323b872dd14610237578063313ce567146102bc5780633eaaf86b146102ed57806370a082311461031857806379ba50971461036f5780638da5cb5b1461038657806395d89b41146103dd578063a293d1e81461046d578063a9059cbb146104b8578063b5931f7c1461051d578063cae9ca5114610568578063d05c78da14610613578063d4ee1d901461065e578063dc39d06d146106b5578063dd62ed3e1461071a578063e6cb901314610791578063f2fde38b146107dc575b600080fd5b34801561012357600080fd5b5061012c61081f565b6040518080602001828103825283818151815260200191508051906020019080838360005b8381101561016c578082015181840152602081019050610151565b50505050905090810190601f1680156101995780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b3480156101b357600080fd5b506101f2600480360381019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803590602001909291905050506108bd565b604051808215151515815260200191505060405180910390f35b34801561021857600080fd5b506102216109af565b6040518082815260200191505060405180910390f35b34801561024357600080fd5b506102a2600480360381019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803590602001909291905050506109fa565b604051808215151515815260200191505060405180910390f35b3480156102c857600080fd5b506102d1610c8a565b604051808260ff1660ff16815260200191505060405180910390f35b3480156102f957600080fd5b50610302610c9d565b6040518082815260200191505060405180910390f35b34801561032457600080fd5b50610359600480360381019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610ca3565b6040518082815260200191505060405180910390f35b34801561037b57600080fd5b50610384610cec565b005b34801561039257600080fd5b5061039b610e8b565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b3480156103e957600080fd5b506103f2610eb0565b6040518080602001828103825283818151815260200191508051906020019080838360005b83811015610432578082015181840152602081019050610417565b50505050905090810190601f16801561045f5780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b34801561047957600080fd5b506104a26004803603810190808035906020019092919080359060200190929190505050610f4e565b6040518082815260200191505060405180910390f35b3480156104c457600080fd5b50610503600480360381019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919080359060200190929190505050610f6a565b604051808215151515815260200191505060405180910390f35b34801561052957600080fd5b5061055260048036038101908080359060200190929190803590602001909291905050506110f3565b6040518082815260200191505060405180910390f35b34801561057457600080fd5b506105f9600480360381019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919080359060200190929190803590602001908201803590602001908080601f0160208091040260200160405190810160405280939291908181526020018383808284378201915050505050509192919290505050611117565b604051808215151515815260200191505060405180910390f35b34801561061f57600080fd5b506106486004803603810190808035906020019092919080359060200190929190505050611366565b6040518082815260200191505060405180910390f35b34801561066a57600080fd5b50610673611397565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b3480156106c157600080fd5b50610700600480360381019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803590602001909291905050506113bd565b604051808215151515815260200191505060405180910390f35b34801561072657600080fd5b5061077b600480360381019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050611521565b6040518082815260200191505060405180910390f35b34801561079d57600080fd5b506107c660048036038101908080359060200190929190803590602001909291905050506115a8565b6040518082815260200191505060405180910390f35b3480156107e857600080fd5b5061081d600480360381019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506115c4565b005b60038054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156108b55780601f1061088a576101008083540402835291602001916108b5565b820191906000526020600020905b81548152906001019060200180831161089857829003601f168201915b505050505081565b600081600760003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020819055508273ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff167f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b925846040518082815260200191505060405180910390a36001905092915050565b6000600660008073ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000205460055403905090565b6000610a45600660008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000205483610f4e565b600660008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002081905550610b0e600760008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000205483610f4e565b600760008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002081905550610bd7600660008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002054836115a8565b600660008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020819055508273ffffffffffffffffffffffffffffffffffffffff168473ffffffffffffffffffffffffffffffffffffffff167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef846040518082815260200191505060405180910390a3600190509392505050565b600460009054906101000a900460ff1681565b60055481565b6000600660008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020549050919050565b600160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff16141515610d4857600080fd5b600160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff166000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a3600160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff166000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055506000600160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b60028054600181600116156101000203166002900480601f016020809104026020016040519081016040528092919081815260200182805460018160011615610100020316600290048015610f465780601f10610f1b57610100808354040283529160200191610f46565b820191906000526020600020905b815481529060010190602001808311610f2957829003601f168201915b505050505081565b6000828211151515610f5f57600080fd5b818303905092915050565b6000610fb5600660003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000205483610f4e565b600660003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002081905550611041600660008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002054836115a8565b600660008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020819055508273ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef846040518082815260200191505060405180910390a36001905092915050565b6000808211151561110357600080fd5b818381151561110e57fe5b04905092915050565b600082600760003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020819055508373ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff167f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b925856040518082815260200191505060405180910390a38373ffffffffffffffffffffffffffffffffffffffff16638f4ffcb1338530866040518563ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401808573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020018481526020018373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200180602001828103825283818151815260200191508051906020019080838360005b838110156112f45780820151818401526020810190506112d9565b50505050905090810190601f1680156113215780820380516001836020036101000a031916815260200191505b5095505050505050600060405180830381600087803b15801561134357600080fd5b505af1158015611357573d6000803e3d6000fd5b50505050600190509392505050565b600081830290506000831480611386575081838281151561138357fe5b04145b151561139157600080fd5b92915050565b600160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614151561141a57600080fd5b8273ffffffffffffffffffffffffffffffffffffffff1663a9059cbb6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff16846040518363ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200182815260200192505050602060405180830381600087803b1580156114de57600080fd5b505af11580156114f2573d6000803e3d6000fd5b505050506040513d602081101561150857600080fd5b8101908080519060200190929190505050905092915050565b6000600760008473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002054905092915050565b600081830190508281101515156115be57600080fd5b92915050565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614151561161f57600080fd5b80600160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550505600a165627a7a7230582014cb94286bd51d908d74ed99d017168aeef735cf2de44eabb3935cc921dacf750029",
  	"nonce": "0x2",
  	"r": "0x10900d90580ce9c809937d8cedc0ae7e487ff94b672b0ecf6443f58dc417f57f",
  	"s": "0x3f702acb8d9bf4f6887376f3c10b3c254d0b0f136dc570df2a114784d35612e2",
  	"to": null,
  	"transactionIndex": "0x5",
  	"type": "0x0",
  	"v": "0x26",
  	"value": "0x0"
  }
  }
   */
  @Test
  void testTransactionToAddress() {
    UInt256 r = UInt256.fromHexString("0x10900d90580ce9c809937d8cedc0ae7e487ff94b672b0ecf6443f58dc417f57f");
    UInt256 s = UInt256.fromHexString("0x3f702acb8d9bf4f6887376f3c10b3c254d0b0f136dc570df2a114784d35612e2");
    Bytes v = Bytes.fromHexString("0x26");
    Transaction tx = Transaction
        .fromEncoded(
            UInt256.valueOf(2),
            Wei.valueOf(UInt256.fromHexString("0x4a817c800")),
            Gas.valueOf(UInt256.fromHexString("0x1abffe")),
            null,
            Wei.valueOf(0),
            Bytes
                .fromHexString(
                    "0x60806040523480156200001157600080fd5b50336000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055506040805190810160405280600381526020017f4332450000000000000000000000000000000000000000000000000000000000815250600290805190602001906200009f929190620001f8565b506040805190810160405280600981526020017f43324520546f6b656e000000000000000000000000000000000000000000000081525060039080519060200190620000ed929190620001f8565b506012600460006101000a81548160ff021916908360ff1602179055506a52b7d2dcc80cd2e400000060058190555060055460066000732ec73685a63f0d3c21dbf4867c3c22a6f170bc1873ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002081905550732ec73685a63f0d3c21dbf4867c3c22a6f170bc1873ffffffffffffffffffffffffffffffffffffffff16600073ffffffffffffffffffffffffffffffffffffffff167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef6005546040518082815260200191505060405180910390a3620002a7565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f106200023b57805160ff19168380011785556200026c565b828001600101855582156200026c579182015b828111156200026b5782518255916020019190600101906200024e565b5b5090506200027b91906200027f565b5090565b620002a491905b80821115620002a057600081600090555060010162000286565b5090565b90565b61168f80620002b76000396000f300608060405260043610610112576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806306fdde0314610117578063095ea7b3146101a757806318160ddd1461020c57806323b872dd14610237578063313ce567146102bc5780633eaaf86b146102ed57806370a082311461031857806379ba50971461036f5780638da5cb5b1461038657806395d89b41146103dd578063a293d1e81461046d578063a9059cbb146104b8578063b5931f7c1461051d578063cae9ca5114610568578063d05c78da14610613578063d4ee1d901461065e578063dc39d06d146106b5578063dd62ed3e1461071a578063e6cb901314610791578063f2fde38b146107dc575b600080fd5b34801561012357600080fd5b5061012c61081f565b6040518080602001828103825283818151815260200191508051906020019080838360005b8381101561016c578082015181840152602081019050610151565b50505050905090810190601f1680156101995780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b3480156101b357600080fd5b506101f2600480360381019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803590602001909291905050506108bd565b604051808215151515815260200191505060405180910390f35b34801561021857600080fd5b506102216109af565b6040518082815260200191505060405180910390f35b34801561024357600080fd5b506102a2600480360381019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803590602001909291905050506109fa565b604051808215151515815260200191505060405180910390f35b3480156102c857600080fd5b506102d1610c8a565b604051808260ff1660ff16815260200191505060405180910390f35b3480156102f957600080fd5b50610302610c9d565b6040518082815260200191505060405180910390f35b34801561032457600080fd5b50610359600480360381019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610ca3565b6040518082815260200191505060405180910390f35b34801561037b57600080fd5b50610384610cec565b005b34801561039257600080fd5b5061039b610e8b565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b3480156103e957600080fd5b506103f2610eb0565b6040518080602001828103825283818151815260200191508051906020019080838360005b83811015610432578082015181840152602081019050610417565b50505050905090810190601f16801561045f5780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b34801561047957600080fd5b506104a26004803603810190808035906020019092919080359060200190929190505050610f4e565b6040518082815260200191505060405180910390f35b3480156104c457600080fd5b50610503600480360381019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919080359060200190929190505050610f6a565b604051808215151515815260200191505060405180910390f35b34801561052957600080fd5b5061055260048036038101908080359060200190929190803590602001909291905050506110f3565b6040518082815260200191505060405180910390f35b34801561057457600080fd5b506105f9600480360381019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919080359060200190929190803590602001908201803590602001908080601f0160208091040260200160405190810160405280939291908181526020018383808284378201915050505050509192919290505050611117565b604051808215151515815260200191505060405180910390f35b34801561061f57600080fd5b506106486004803603810190808035906020019092919080359060200190929190505050611366565b6040518082815260200191505060405180910390f35b34801561066a57600080fd5b50610673611397565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b3480156106c157600080fd5b50610700600480360381019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803590602001909291905050506113bd565b604051808215151515815260200191505060405180910390f35b34801561072657600080fd5b5061077b600480360381019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050611521565b6040518082815260200191505060405180910390f35b34801561079d57600080fd5b506107c660048036038101908080359060200190929190803590602001909291905050506115a8565b6040518082815260200191505060405180910390f35b3480156107e857600080fd5b5061081d600480360381019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506115c4565b005b60038054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156108b55780601f1061088a576101008083540402835291602001916108b5565b820191906000526020600020905b81548152906001019060200180831161089857829003601f168201915b505050505081565b600081600760003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020819055508273ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff167f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b925846040518082815260200191505060405180910390a36001905092915050565b6000600660008073ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000205460055403905090565b6000610a45600660008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000205483610f4e565b600660008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002081905550610b0e600760008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000205483610f4e565b600760008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002081905550610bd7600660008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002054836115a8565b600660008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020819055508273ffffffffffffffffffffffffffffffffffffffff168473ffffffffffffffffffffffffffffffffffffffff167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef846040518082815260200191505060405180910390a3600190509392505050565b600460009054906101000a900460ff1681565b60055481565b6000600660008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020549050919050565b600160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff16141515610d4857600080fd5b600160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff166000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a3600160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff166000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055506000600160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b60028054600181600116156101000203166002900480601f016020809104026020016040519081016040528092919081815260200182805460018160011615610100020316600290048015610f465780601f10610f1b57610100808354040283529160200191610f46565b820191906000526020600020905b815481529060010190602001808311610f2957829003601f168201915b505050505081565b6000828211151515610f5f57600080fd5b818303905092915050565b6000610fb5600660003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000205483610f4e565b600660003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002081905550611041600660008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002054836115a8565b600660008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020819055508273ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef846040518082815260200191505060405180910390a36001905092915050565b6000808211151561110357600080fd5b818381151561110e57fe5b04905092915050565b600082600760003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020819055508373ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff167f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b925856040518082815260200191505060405180910390a38373ffffffffffffffffffffffffffffffffffffffff16638f4ffcb1338530866040518563ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401808573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020018481526020018373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200180602001828103825283818151815260200191508051906020019080838360005b838110156112f45780820151818401526020810190506112d9565b50505050905090810190601f1680156113215780820380516001836020036101000a031916815260200191505b5095505050505050600060405180830381600087803b15801561134357600080fd5b505af1158015611357573d6000803e3d6000fd5b50505050600190509392505050565b600081830290506000831480611386575081838281151561138357fe5b04145b151561139157600080fd5b92915050565b600160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614151561141a57600080fd5b8273ffffffffffffffffffffffffffffffffffffffff1663a9059cbb6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff16846040518363ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200182815260200192505050602060405180830381600087803b1580156114de57600080fd5b505af11580156114f2573d6000803e3d6000fd5b505050506040513d602081101561150857600080fd5b8101908080519060200190929190505050905092915050565b6000600760008473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002054905092915050565b600081830190508281101515156115be57600080fd5b92915050565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614151561161f57600080fd5b80600160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550505600a165627a7a7230582014cb94286bd51d908d74ed99d017168aeef735cf2de44eabb3935cc921dacf750029"),
            r.toUnsignedBigInteger(),
            s.toUnsignedBigInteger(),
            v.toInt());
    assertEquals(
        Hash.fromHexString("0x24158c06c57f4a2814b8b31809378c4a457028d57dcf3c29dfbbc13f850ebfea"),
        tx.getHash());

    assertEquals("0xc82a6220398714f74e2d929309f4c5b1d4f7b0f6", tx.getSender().toHexString());
    Address result = Address.fromTransaction(tx);
    assertEquals("0xceb46d90598cd81c4e10557c0050a27569e2163e", result.toHexString());
  }
}
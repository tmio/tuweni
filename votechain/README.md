# Voter chain

This is a POC of a blockchain with a simple, pluggable consensus.

## Configuration

### Initial premine
Blockchains start with pre-mined accounts that are given a certain set of positive or zero amounts.

Blockchains MUST have at least one valid account premined.

Account amounts sum MUST be greater than zero.

### Miner rewards
Blockchains MUST define a miner reward, positive or zero.

The reward is sent to the account designated as the coinbase of the block.

### Operations

The blockchains may define a number of operations. You can create additional operations as you see fit.
You may also restrict operations to a subset of the ones offered below.

#### Transfer
Transfer a positive amount from the current signing account to a target account.

#### Mint
Create new money to be added to the current signing account.

#### Burn
Burn money associated with the current account.

#### Sign
Sign 32 bytes.
This enshrines the bytes in the chain as signed by the chain.

### Operation parameters
#### Gas price
Each operation may have fixed gas prices. For simplicity sake, we're just assigning a price per operation.

#### Vote parameters
Each operation may define how it will be validated by the participants of the chain.

##### Thresholds
Each operation may define a threshold of number of votes or capital associated with an operation to be eligible to be included into a block.

For example, a signing operation may require a majority of the capital to be voting in favor of the operation.

##### Consensus
This is a corner case of thresholds.
Operations may also require absolute consensus, ie all participants must sign in favor of the operation for it to be included.

### Consensus

#### Transactions

#### Attestations

#### Block minting

#### Block verification

#### Heaviest chain algorithm




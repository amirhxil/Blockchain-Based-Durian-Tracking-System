// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract DurianContract {
    address public owner;

    enum UserRole {
        Unassigned,
        Farmer,
        Distributor,
        Retailer,
        Consumer
    }

    enum UserState {
        Active,
        Deactivated
    }
    enum DurianState {
        Harvested,
        AtDistributor,
        AtRetailer,
        Sold,
        Spoiled
    }

    enum Action {
        Harvested,
        Transferred,
        UpdatedState,
        Removed
    }

    struct User {
        address userAddress;
        string name;
        string companyAddress;
        string companyName;
        UserRole role;
        UserState state;
    }

    struct Durian {
        uint256 batchCode;
        string durianType;
        DurianState state;
        uint256 weightInGram;
        address currentOwner;
        string farmLocation;
        uint256 arriveAtDistributorTime;
        uint256 arriveAtRetailerTime;
        uint256 timestamp;
        uint256 addedTimestamp;
        string distributorName;
        string retailerName;
    }

    struct Transaction {
        address from;
        address to;
        uint256 durianId;
        uint256 timestamp;
        Action action;
    }

    mapping(address => User) public users;
    mapping(uint256 => Durian) public durians;
    mapping(address => bool) public authorizedFarmers;
    mapping(uint256 => bool) private existedBatchCodes;
    mapping(uint256 => uint256[]) public durianStateTimestamps;

    event DurianHarvested(uint256 indexed batchCode, uint256 timestamp);
    event OwnershipTransferred(
        uint256 indexed batchCode,
        address indexed from,
        address indexed to,
        DurianState newState,
        uint256 timestamp
    );

    uint256[] public batchCodes;
    Transaction[] public transactions;

    modifier onlyAuthorizedFarmer() {
        require(
            users[msg.sender].role == UserRole.Farmer,
            "Not an authorized farmer"
        );
        _;
    }

    modifier uniqueBatchCode(uint256 batchCode) {
        require(
            durians[batchCode].batchCode == 0,
            "Batch code already exists."
        );
        _;
    }

    constructor() {
        owner = msg.sender;
    }

    function recordHarvestDurian(
        uint256 _batchCode,
        string memory durianType,
        string memory farmLocation,
        uint256 weightInGram
    ) public onlyAuthorizedFarmer uniqueBatchCode(_batchCode) {
        Durian storage newDurian = durians[_batchCode];
        newDurian.batchCode = _batchCode;
        newDurian.durianType = durianType;
        newDurian.state = DurianState.Harvested;
        newDurian.currentOwner = msg.sender;
        newDurian.farmLocation = farmLocation;
        newDurian.weightInGram = weightInGram;
        newDurian.timestamp = block.timestamp;
        newDurian.addedTimestamp = block.timestamp;

        batchCodes.push(_batchCode);

        existedBatchCodes[_batchCode] = true;
        durianStateTimestamps[_batchCode].push(block.timestamp);

        transactions.push(
            Transaction({
                from: msg.sender,
                to: msg.sender,
                durianId: _batchCode,
                timestamp: block.timestamp,
                action: Action.Harvested
            })
        );

        emit DurianHarvested(_batchCode, newDurian.timestamp);
    }

    function isBatchCodeExisted(uint256 batchCode) public view returns (bool) {
        return existedBatchCodes[batchCode];
    }

    function transferDurianOwnership(uint256 _batchCode, address newOwner) public {
        require(_batchCode != 0, "Durian batch code is invalid.");
        require(durians[_batchCode].currentOwner == msg.sender, "You are not the current owner.");
        require(newOwner != address(0), "New owner address is invalid.");
        require(users[newOwner].role != UserRole.Unassigned, "New owner must have a valid role.");

        durians[_batchCode].currentOwner = newOwner;
        uint256 currentTimestamp = block.timestamp;
        durians[_batchCode].timestamp = currentTimestamp;

        UserRole newOwnerRole = users[newOwner].role;
        updateDurianState(_batchCode, newOwnerRole, newOwner);


        transactions.push(
            Transaction({
                from: msg.sender,
                to: newOwner,
                durianId: _batchCode,
                timestamp: currentTimestamp,
                action: Action.Transferred
            })
        );

        emit OwnershipTransferred(_batchCode, msg.sender, newOwner, durians[_batchCode].state, currentTimestamp);
    }

    function updateDurianState(uint256 _batchCode, UserRole role, address newOwner) internal {
        DurianState currentState = durians[_batchCode].state;

        if (role == UserRole.Distributor && currentState != DurianState.AtDistributor) {
            durians[_batchCode].state = DurianState.AtDistributor;
            durians[_batchCode].arriveAtDistributorTime = block.timestamp;
            durians[_batchCode].distributorName = users[newOwner].companyName;
            durianStateTimestamps[_batchCode].push(block.timestamp);
        } else if (role == UserRole.Retailer && currentState != DurianState.AtRetailer) {
            durians[_batchCode].state = DurianState.AtRetailer;
            durians[_batchCode].arriveAtRetailerTime = block.timestamp;
            durians[_batchCode].retailerName = users[newOwner].companyName;
            durianStateTimestamps[_batchCode].push(block.timestamp);
        } else if (role == UserRole.Consumer && currentState != DurianState.Sold) {
            durians[_batchCode].state = DurianState.Sold;
            durianStateTimestamps[_batchCode].push(block.timestamp);
        } else {
            revert("Invalid role or state transition.");
        }
    }

    function removeDurian(uint256 _batchCode) public onlyAuthorizedFarmer {
        require(
            durians[_batchCode].state == DurianState.Spoiled,
            "Only spoiled durians can be removed."
        );

        existedBatchCodes[_batchCode] = true;

        delete durians[_batchCode];

        transactions.push(
            Transaction({
                from: msg.sender,
                to: msg.sender,
                durianId: _batchCode,
                timestamp: block.timestamp,
                action: Action.Removed
            })
        );
    }

    function getTransactions() public view returns (Transaction[] memory) {
        return transactions;
    }

    function getDurian(uint256 batchCode) public view returns (Durian memory) {
        require(durians[batchCode].batchCode != 0, "Durian not found");
        return durians[batchCode];
    }

    function getBatchCodes() public view returns (uint256[] memory) {
        return batchCodes;
    }

    function getDurianStateTimestamps(uint256 _batchCode)
    public
    view
    returns (uint256[] memory)
    {
        return durianStateTimestamps[_batchCode];
    }

    function getUserDurians(address user)
    public
    view
    returns (Durian[] memory)
    {
        uint256 userDurianCount = 0;

        for (uint256 i = 0; i < batchCodes.length; i++) {
            uint256 batchCode = batchCodes[i];
            if (durians[batchCode].currentOwner == user) {
                userDurianCount++;
            }
        }

        Durian[] memory userDurians = new Durian[](userDurianCount);
        uint256 index = 0;

        for (uint256 i = 0; i < batchCodes.length; i++) {
            uint256 batchCode = batchCodes[i];
            if (durians[batchCode].currentOwner == user) {
                userDurians[index] = durians[batchCode];
                index++;
            }
        }

        return userDurians;
    }

    function getSpoiledDurians() public view returns (Durian[] memory) {
        uint256 spoiledCount = 0;

        for (uint256 i = 0; i < batchCodes.length; i++) {
            uint256 batchCode = batchCodes[i];
            if (durians[batchCode].state == DurianState.Spoiled) {
                spoiledCount++;
            }
        }

        Durian[] memory spoiledDurians = new Durian[](spoiledCount);
        uint256 index = 0;

        for (uint256 i = 0; i < batchCodes.length; i++) {
            uint256 batchCode = batchCodes[i];
            if (durians[batchCode].state == DurianState.Spoiled) {
                spoiledDurians[index] = durians[batchCode];
                index++;
            }
        }

        return spoiledDurians;
    }

    function updateSpoiledState(uint256 _batchCode) public {
        Durian storage durian = durians[_batchCode];

        require(durian.batchCode != 0, "Durian does not exist.");
        require(
            durian.state != DurianState.Spoiled,
            "Durian is already spoiled."
        );

        durian.state = DurianState.Spoiled;

        transactions.push(
            Transaction({
                from: msg.sender,
                to: msg.sender,
                durianId: _batchCode,
                timestamp: block.timestamp,
                action: Action.UpdatedState
            })
        );
    }

    function isProfileSet(address userAddress) public view returns (bool) {
        return bytes(users[userAddress].name).length > 0;
    }

    function setUser(
        string memory name,
        address userAddress,
        string memory companyAddress,
        string memory companyName,
        UserRole role
    ) public {
        users[msg.sender] = User({
            userAddress: userAddress,
            companyAddress: companyAddress,
            companyName: companyName,
            name: name,
            role: role,
            state: UserState.Active
        });
    }

    function generateTrackingReport(uint256 _batchCode)
    public
    view
    returns (
        uint256 batchCode,
        string memory durianType,
        DurianState state,
        uint256 harvestDate,
        string memory farmLocation,
        string memory distributorName,
        string memory retailerName
    )
    {
        require(_batchCode != 0, "Durian batch does not exist.");
        Durian memory durian = durians[_batchCode];

        return (
            durian.batchCode,
            durian.durianType,
            durian.state,
            durian.timestamp,
            durian.farmLocation,
            durian.distributorName,
            durian.retailerName
        );
    }
}

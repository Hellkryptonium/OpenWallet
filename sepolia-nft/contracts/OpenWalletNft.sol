// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import {ERC721} from "@openzeppelin/contracts/token/ERC721/ERC721.sol";
import {ERC721URIStorage} from "@openzeppelin/contracts/token/ERC721/extensions/ERC721URIStorage.sol";
import {Ownable} from "@openzeppelin/contracts/access/Ownable.sol";

contract OpenWalletNft is ERC721URIStorage, Ownable {
    uint256 public nextTokenId;

    constructor(string memory name_, string memory symbol_, address initialOwner)
        ERC721(name_, symbol_)
        Ownable(initialOwner)
    {}

    function mintTo(address to, string calldata tokenUri) external onlyOwner returns (uint256) {
        uint256 id = nextTokenId++;
        _safeMint(to, id);
        _setTokenURI(id, tokenUri);
        return id;
    }
}

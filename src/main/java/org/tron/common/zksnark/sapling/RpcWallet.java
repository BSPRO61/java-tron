package org.tron.common.zksnark.sapling;

import static org.tron.common.zksnark.sapling.zip32.ExtendedSpendingKey.ZIP32_HARDENED_KEY_LIMIT;

import java.util.List;
import org.tron.common.zksnark.sapling.Wallet.SaplingNoteEntry;
import org.tron.common.zksnark.sapling.address.IncomingViewingKey;
import org.tron.common.zksnark.sapling.address.PaymentAddress;
import org.tron.common.zksnark.sapling.transaction.Recipient;
import org.tron.common.zksnark.sapling.utils.KeyIo;
import org.tron.common.zksnark.sapling.walletdb.CKeyMetadata;
import org.tron.common.zksnark.sapling.zip32.ExtendedSpendingKey;
import org.tron.common.zksnark.sapling.zip32.HDSeed;

public class RpcWallet {

  //功能
//  { "wallet",             "z_getnewaddress",          &z_getnewaddress,          true  },
//  { "wallet",             "z_sendmany",               &z_sendmany,               false },
//  { "wallet",             "z_importkey",              &z_importkey,              true  },

  //查询
//  { "wallet",             "z_listunspent",            &z_listunspent,            false },
//  { "wallet",             "z_getbalance",             &z_getbalance,             false },
//  { "wallet",             "z_gettotalbalance",        &z_gettotalbalance,        false },
//  { "wallet",             "z_listaddresses",          &z_listaddresses,          true  },
//  { "wallet",             "z_listreceivedbyaddress",  &z_listreceivedbyaddress,  false },
//  { "disclosure",         "z_getpaymentdisclosure",   &z_getpaymentdisclosure,   true  },
//  { "disclosure",         "z_validatepaymentdisclosure", &z_validatepaymentdisclosure, true }

  //其他
//  { "wallet",             "z_mergetoaddress",         &z_mergetoaddress,         false },
//  { "wallet",             "z_exportkey",              &z_exportkey,              true  },
//  { "wallet",             "z_exportviewingkey",       &z_exportviewingkey,       true  },
//  { "wallet",             "z_exportwallet",           &z_exportwallet,           true  },
//  { "wallet",             "z_importwallet",           &z_importwallet,           true  },


  public void z_getnewaddress() {
    //seed
    //AccountCounter

    // Create new metadata
    long nCreationTime = System.currentTimeMillis();
    CKeyMetadata metadata = new CKeyMetadata(nCreationTime);

    // Try to get the seed
    HDSeed seed = KeyStore.seed;
    if (seed == null) {
      throw new RuntimeException("CWallet::GenerateNewSaplingZKey(): HD seed not found");
    }

    ExtendedSpendingKey m = ExtendedSpendingKey.Master(seed);
    int bip44CoinType = Params.BIP44CoinType;

    // We use a fixed keypath scheme of m/32'/coin_type'/account'
    // Derive m/32'
    ExtendedSpendingKey m_32h = m.Derive(32 | ZIP32_HARDENED_KEY_LIMIT);
    // Derive m/32'/coin_type'
    ExtendedSpendingKey m_32h_cth = m_32h.Derive(bip44CoinType | ZIP32_HARDENED_KEY_LIMIT);

    // Derive account key at next index, skip keys already known to the wallet
    ExtendedSpendingKey xsk = null;

    while (xsk == null || KeyStore.HaveSaplingSpendingKey(xsk.getExpsk().full_viewing_key())) {
      //这里使用累加器，生成d账户
      xsk = m_32h_cth.Derive(HdChain.saplingAccountCounter | ZIP32_HARDENED_KEY_LIMIT);
      metadata.hdKeypath = "m/32'/" + bip44CoinType + "'/" + HdChain.saplingAccountCounter + "'";
      metadata.seedFp = HdChain.seedFp;
      // Increment childkey index
      HdChain.saplingAccountCounter++;
    }

    // Update the chain model in the database
//    if (fFileBacked && !CWalletDB(strWalletFile).WriteHDChain(hdChain))
//      throw new RuntimeException("CWallet::GenerateNewSaplingZKey(): Writing HD chain model failed");

    IncomingViewingKey ivk = xsk.getExpsk().full_viewing_key().in_viewing_key();
    Wallet.mapSaplingZKeyMetadata.put(ivk, metadata);

    PaymentAddress addr = xsk.DefaultAddress();
    if (!Wallet.AddSaplingZKey(xsk, addr)) {
      throw new RuntimeException("CWallet::GenerateNewSaplingZKey(): AddSaplingZKey failed");
    }
    // return default sapling payment address.

    System.out.println(KeyIo.EncodePaymentAddress(addr);
  }

  public void z_sendmany() {
    String fromAddress = "";
    String taddr = "";
    PaymentAddress zaddr;
    if (isValidTAddress(fromAddress)) {
      //todo
      taddr = "";
    } else if (isValidShieldAddress(fromAddress)) {
      zaddr = KeyIo.DecodePaymentAddress(fromAddress);
      if (!Wallet.HaveSpendingKeyForPaymentAddress(zaddr)) {
        throw new RuntimeException("");
      }
    } else {
      throw new RuntimeException("unknown address type ");
    }

    //todo：支持多个输出？
    List<Recipient> t_outputs_ = null;
    List<Recipient> z_outputs_ = null;
    ShieldSendCoin sendmany =
        new ShieldSendCoin(fromAddress, t_outputs_, z_outputs_);
    sendmany.main_impl();
  }


  //todo:
  private boolean isValidTAddress(String address) {
    return true;
  }

  //todo:
  private boolean isValidShieldAddress(String address) {
    return true;
  }

  //扫描交易，获得address相关的note
  UniValue z_importkey(const UniValue&params, bool fHelp) {
    // We want to scan for transactions and notes
    if (fRescan) {
      Wallet.ScanForWalletTransactions(chainActive[nRescanHeight], true);
    }
  }

  //reindex/rescan to find  the old transactions.应该有接口，直接扫描过去的交易
  //这里的received，是从本地存储的note中过滤，不是从fullnode里查。
  //tron，需要另写一个receive方法，指定交易id，解析获得note。
  UniValue z_listreceivedbyaddress(const UniValue&params, bool fHelp) {

    vector<SaplingNoteEntry> saplingEntries;
    pwalletMain -> GetFilteredNotes(sproutEntries, saplingEntries, fromaddress, nMinDepth, false,
        false);

    set<pair<PaymentAddress, uint256>> nullifierSet;
    auto hasSpendingKey = boost::apply_visitor (HaveSpendingKeyForPaymentAddress(pwalletMain), zaddr)
    ;
    if (hasSpendingKey) {
      nullifierSet = pwalletMain -> GetNullifiersForAddresses({zaddr});
    }
  }


  /**
   * 解密交易相关的信息 RPC call to generate a payment disclosure
   */
  UniValue z_getpaymentdisclosure(const UniValue&params, bool fHelp) {

    PaymentDisclosure pd (wtx.joinSplitPubKey, key, info, msg );
  }

  /**
   * 校验解密信息正确性 RPC call to validate a payment disclosure data blob.
   */
  UniValue z_validatepaymentdisclosure(const UniValue&params, bool fHelp) {

  }

}

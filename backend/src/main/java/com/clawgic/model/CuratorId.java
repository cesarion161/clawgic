package com.clawgic.model;

import java.io.Serializable;
import java.util.Objects;

public class CuratorId implements Serializable {
    private String wallet;
    private Integer marketId;

    public CuratorId() {
    }

    public CuratorId(String wallet, Integer marketId) {
        this.wallet = wallet;
        this.marketId = marketId;
    }

    public String getWallet() {
        return wallet;
    }

    public void setWallet(String wallet) {
        this.wallet = wallet;
    }

    public Integer getMarketId() {
        return marketId;
    }

    public void setMarketId(Integer marketId) {
        this.marketId = marketId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CuratorId that = (CuratorId) o;
        return Objects.equals(wallet, that.wallet) && Objects.equals(marketId, that.marketId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wallet, marketId);
    }
}

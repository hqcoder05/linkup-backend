package com.linkup.connection;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ConnectionId implements Serializable {
    @Column(name = "requester_id")
    private Long requesterId;

    @Column(name = "addressee_id")
    private Long addresseeId;

    public ConnectionId() {
    }

    public ConnectionId(Long requesterId, Long addresseeId) {
        this.requesterId = requesterId;
        this.addresseeId = addresseeId;
    }

    public Long getRequesterId() { return requesterId; }
    public void setRequesterId(Long requesterId) { this.requesterId = requesterId; }
    public Long getAddresseeId() { return addresseeId; }
    public void setAddresseeId(Long addresseeId) { this.addresseeId = addresseeId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConnectionId that)) return false;
        return Objects.equals(requesterId, that.requesterId) && Objects.equals(addresseeId, that.addresseeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requesterId, addresseeId);
    }
}

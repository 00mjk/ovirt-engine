package org.ovirt.engine.core.common.businessentities;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.compat.Guid;

public class VmCheckpoint implements Queryable, BusinessEntity<Guid> {

    private static final long serialVersionUID = 1155292523061178984L;

    private Guid id;

    private Guid vmId;

    private Guid parentId;

    private Date creationDate;

    private List<DiskImage> disks;

    private String checkpointXml;

    public Guid getId() {
        return id;
    }

    public void setId(Guid id) {
        this.id = id;
    }

    public Guid getVmId() {
        return vmId;
    }

    public void setVmId(Guid vmId) {
        this.vmId = vmId;
    }

    public Guid getParentId() {
        return parentId;
    }

    public void setParentId(Guid parentId) {
        this.parentId = parentId;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public List<DiskImage> getDisks() {
        return disks;
    }

    public void setDisks(List<DiskImage> disks) {
        this.disks = disks;
    }

    public String getCheckpointXml() {
        return checkpointXml;
    }

    public void setCheckpointXml(String checkpointXml) {
        this.checkpointXml = checkpointXml;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                vmId,
                parentId,
                creationDate,
                checkpointXml
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VmCheckpoint)) {
            return false;
        }
        VmCheckpoint other = (VmCheckpoint) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(vmId, other.vmId)
                && Objects.equals(parentId, other.parentId)
                && Objects.equals(creationDate, other.creationDate)
                && Objects.equals(checkpointXml, other.checkpointXml);
    }

    @Override
    public Object getQueryableId() {
        return id;
    }
}

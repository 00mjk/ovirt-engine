package org.ovirt.engine.core.dao.gluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.VdsStatic;
import org.ovirt.engine.core.common.businessentities.gluster.AccessProtocol;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeType;
import org.ovirt.engine.core.common.businessentities.gluster.TransportType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.BaseDAOTestCase;

/**
 * Tests for Gluster Volume DAO
 */
public class GlusterVolumeDaoTest extends BaseDAOTestCase {
    private static final Guid SERVER_ID = new Guid("afce7a39-8e8c-4819-ba9c-796d316592e6");
    private static final Guid CLUSTER_ID = new Guid("b399944a-81ab-4ec5-8266-e19ba7c3c9d1");
    private static final Guid EXISTING_VOL_DIST_ID = new Guid("0c3f45f6-3fe9-4b35-a30c-be0d1a835ea8");
    private static final Guid EXISTING_VOL_REPL_ID = new Guid("b2cb2f73-fab3-4a42-93f0-d5e4c069a43e");
    private static final String EXISTING_VOL_REPL_NAME = "test-vol-replicate-1";
    private static final String NEW_VOL_NAME = "test-new-vol-1";
    private GlusterVolumeDao dao;
    private VdsStatic server;
    private GlusterVolumeEntity existingDistVol;
    private GlusterVolumeEntity existingReplVol;
    private GlusterVolumeEntity newVolume;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        dao = dbFacade.getGlusterVolumeDao();
        server = dbFacade.getVdsStaticDAO().get(SERVER_ID);
        existingDistVol = dao.getById(EXISTING_VOL_DIST_ID);
        existingReplVol = dao.getById(EXISTING_VOL_REPL_ID);
    }

    @Test
    public void testSaveAndGetById() {
        GlusterVolumeEntity volume = dao.getByName(CLUSTER_ID, NEW_VOL_NAME);
        assertNull(volume);

        newVolume = insertTestVolume();
        volume = dao.getById(newVolume.getId());

        assertNotNull(volume);
        assertEquals(newVolume, volume);
    }

    @Test
    public void testGetByName() {
        newVolume = insertTestVolume();
        GlusterVolumeEntity volume = dao.getByName(CLUSTER_ID, NEW_VOL_NAME);

        assertNotNull(volume);
        assertEquals(newVolume, volume);
    }

    @Test
    public void testGetByClusterId() {
        List<GlusterVolumeEntity> volumes = dao.getByClusterId(CLUSTER_ID);

        assertTrue(volumes != null);
        assertTrue(volumes.size() == 2);
        assertTrue(volumes.contains(existingDistVol));
        assertTrue(volumes.contains(existingReplVol));
    }

    @Test
    public void testGetAllWithQuery() {
        List<GlusterVolumeEntity> volumes =
                dao.getAllWithQuery("select * from gluster_volumes where vol_type = '"
                        + GlusterVolumeType.DISTRIBUTED_REPLICATE.name() + "'");

        assertTrue(volumes != null);
        assertTrue(volumes.size() == 1);
        assertEquals(existingReplVol, volumes.get(0));
    }

    @Test
    public void testRemove() {
        dao.remove(EXISTING_VOL_DIST_ID);
        List<GlusterVolumeEntity> volumes = dao.getByClusterId(CLUSTER_ID);

        assertTrue(volumes.size() == 1);
        assertFalse(volumes.contains(existingDistVol));
        assertTrue(volumes.contains(existingReplVol));
    }

    @Test
    public void testRemoveByName() {
        dao.removeByName(CLUSTER_ID, EXISTING_VOL_REPL_NAME);
        List<GlusterVolumeEntity> volumes = dao.getByClusterId(CLUSTER_ID);

        assertTrue(volumes.size() == 1);
        assertTrue(volumes.contains(existingDistVol));
        assertFalse(volumes.contains(existingReplVol));
    }

    @Test
    public void testUpdateVolumeStatus() {
        assertTrue(existingDistVol.isOnline());

        dao.updateVolumeStatus(existingDistVol.getId(), GlusterVolumeStatus.DOWN);
        GlusterVolumeEntity volume = dao.getById(existingDistVol.getId());

        assertNotNull(volume);
        assertFalse(volume.isOnline());

        assertFalse(volume.equals(existingDistVol));
        existingDistVol.setStatus(GlusterVolumeStatus.DOWN);
        assertEquals(existingDistVol, volume);
    }

    @Test
    public void testUpdateVolumeStatusByName() {
        assertTrue(existingDistVol.isOnline());

        dao.updateVolumeStatusByName(existingDistVol.getClusterId(),
                existingDistVol.getName(),
                GlusterVolumeStatus.DOWN);
        GlusterVolumeEntity volume = dao.getById(existingDistVol.getId());

        assertNotNull(volume);
        assertFalse(volume.isOnline());

        assertFalse(volume.equals(existingDistVol));
        existingDistVol.setStatus(GlusterVolumeStatus.DOWN);
        assertEquals(existingDistVol, volume);
    }

    @Test
    public void testReplicateCount() {
        GlusterVolumeEntity volume = dao.getById(EXISTING_VOL_REPL_ID);
        int replicaCount = volume.getReplicaCount();

        assertTrue(replicaCount != 0);
        assertEquals(2, replicaCount);

        dao.updateReplicaCount(EXISTING_VOL_REPL_ID, 3);

        GlusterVolumeEntity volumeAfter = dao.getById(EXISTING_VOL_REPL_ID);
        assertNotNull(volumeAfter);

        replicaCount = volumeAfter.getReplicaCount();
        assertTrue(replicaCount != 0);
        assertEquals(3, replicaCount);
    }

    @Test
    public void testStripeCount() {
        GlusterVolumeEntity volume = dao.getById(EXISTING_VOL_REPL_ID);
        int replicaCount = volume.getReplicaCount();

        assertTrue(replicaCount != 0);
        assertEquals(2, replicaCount);

        dao.updateReplicaCount(EXISTING_VOL_REPL_ID, 4);

        GlusterVolumeEntity volumeAfter = dao.getById(EXISTING_VOL_REPL_ID);
        assertNotNull(volumeAfter);

        replicaCount = volumeAfter.getReplicaCount();
        assertTrue(replicaCount != 0);
        assertEquals(4, replicaCount);
    }

    public void testAddAccessProtocol() {
        Set<AccessProtocol> protocols = existingDistVol.getAccessProtocols();
        assertEquals(1, protocols.size());
        assertFalse(protocols.contains(AccessProtocol.NFS));

        dao.addAccessProtocol(EXISTING_VOL_DIST_ID, AccessProtocol.NFS);

        GlusterVolumeEntity volumeAfter = dao.getById(EXISTING_VOL_DIST_ID);
        assertNotNull(volumeAfter);

        protocols = volumeAfter.getAccessProtocols();
        assertEquals(2, protocols.size());
        assertTrue(protocols.contains(AccessProtocol.NFS));

        assertFalse(volumeAfter.equals(existingDistVol));
        existingDistVol.addAccessProtocol(AccessProtocol.NFS);
        assertEquals(volumeAfter, existingDistVol);
    }

    @Test
    public void testRemoveAccessProtocol() {
        Set<AccessProtocol> protocols = existingReplVol.getAccessProtocols();
        assertEquals(2, protocols.size());
        assertTrue(protocols.contains(AccessProtocol.NFS));

        dao.removeAccessProtocol(EXISTING_VOL_REPL_ID, AccessProtocol.NFS);

        GlusterVolumeEntity volumeAfter = dao.getById(EXISTING_VOL_REPL_ID);
        assertNotNull(volumeAfter);

        protocols = volumeAfter.getAccessProtocols();
        assertEquals(1, protocols.size());
        assertFalse(protocols.contains(AccessProtocol.NFS));

        assertFalse(volumeAfter.equals(existingReplVol));
        existingReplVol.removeAccessProtocol(AccessProtocol.NFS);
        assertEquals(volumeAfter, existingReplVol);
    }

    @Test
    public void testAddTransportType() {
        Set<TransportType> transportTypes = existingDistVol.getTransportTypes();
        assertEquals(1, transportTypes.size());
        assertFalse(transportTypes.contains(TransportType.RDMA));

        dao.addTransportType(EXISTING_VOL_DIST_ID, TransportType.RDMA);

        GlusterVolumeEntity volumeAfter = dao.getById(EXISTING_VOL_DIST_ID);
        assertNotNull(volumeAfter);

        transportTypes = volumeAfter.getTransportTypes();
        assertEquals(2, transportTypes.size());
        assertTrue(transportTypes.contains(TransportType.RDMA));

        assertFalse(volumeAfter.equals(existingDistVol));
        existingDistVol.addTransportType(TransportType.RDMA);
        assertEquals(volumeAfter, existingDistVol);
    }

    @Test
    public void testRemoveTransportType() {
        Set<TransportType> transportTypes = existingReplVol.getTransportTypes();
        assertEquals(2, transportTypes.size());
        assertTrue(transportTypes.contains(TransportType.RDMA));

        dao.removeTransportType(EXISTING_VOL_REPL_ID, TransportType.RDMA);

        GlusterVolumeEntity volumeAfter = dao.getById(EXISTING_VOL_REPL_ID);
        assertNotNull(volumeAfter);

        transportTypes = volumeAfter.getTransportTypes();
        assertEquals(1, transportTypes.size());
        assertFalse(transportTypes.contains(TransportType.RDMA));

        assertFalse(volumeAfter.equals(existingReplVol));
        existingReplVol.removeTransportType(TransportType.RDMA);
        assertEquals(volumeAfter, existingReplVol);
    }

    private GlusterVolumeEntity insertTestVolume() {
        Guid volumeId = Guid.NewGuid();

        GlusterVolumeEntity volume = new GlusterVolumeEntity();
        volume.setName(NEW_VOL_NAME);
        volume.setClusterId(CLUSTER_ID);
        volume.setId(volumeId);
        volume.setVolumeType(GlusterVolumeType.DISTRIBUTE);
        volume.addTransportType(TransportType.TCP);
        volume.setReplicaCount(0);
        volume.setStripeCount(0);
        volume.setStatus(GlusterVolumeStatus.UP);
        volume.setOption("auth.allow", "*");
        volume.addAccessProtocol(AccessProtocol.GLUSTER);
        volume.addAccessProtocol(AccessProtocol.NFS);

        GlusterBrickEntity brick =
                new GlusterBrickEntity(volumeId, server, "/export/testVol1", GlusterBrickStatus.UP);
        volume.addBrick(brick);

        dao.save(volume);
        return volume;
    }
}

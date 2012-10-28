package org.ovirt.engine.core.dao;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.VmNetworkInterface;
import org.ovirt.engine.core.compat.Guid;

/**
 * <code>VmNetworkInterfaceDAO</code> defines a type for performing CRUD operations on instances of
 * {@link VmNetworkInterface}.
 */
public interface VmNetworkInterfaceDAO extends GenericDao<VmNetworkInterface, Guid> {
    /**
     * Retrieves all interfaces for the given VM id.
     *
     * @param id
     *            the Vm id
     * @return the list of interfaces
     */
    List<VmNetworkInterface> getAllForVm(Guid id);

    /**
     * Retrieves all interfaces for the given VM id,
     * with optional filtering
     *
     * @param id
     *            the Vm id
     * @param userID
     *            the ID of the user requesting the information
     * @param isFiltered
     *            Whether the results should be filtered according to the user's permissions
     * @return the list of interfaces
     */
    List<VmNetworkInterface> getAllForVm(Guid id, Guid userID, boolean isFiltered);

    /**
     * Retrieves all interfaces for the given template id.
     *
     * @param id
     *            the template id
     * @return the list of interfaces
     */
    List<VmNetworkInterface> getAllForTemplate(Guid id);

    /**
     * Retrieves all interfaces for the given template id with optional filtering.
     *
     * @param id
     *           the template id
     * @param userID
     *            the ID of the user requesting the information
     * @param isFiltered
     *            Whether the results should be filtered according to the user's permissions
     * @return the list of interfaces
     */
    List<VmNetworkInterface> getAllForTemplate(Guid id, Guid userID, boolean isFiltered);

    /**
     * Retrieves the VmNetworkInterfaces that the given network is attached to.
     *
     * @param networkId
     *            the network
     * @return the list of VmNetworkInterfaces
     */
    List<VmNetworkInterface> getAllForNetwork(Guid networkId);

    /**
     * Retrieves the VmTemplate Network Interfaces that the given network is attached to.
     *
     * @param networkId
     *            the network
     * @return the list of VmNetworkInterfaces
     */
    List<VmNetworkInterface> getAllForTemplatesByNetwork(Guid networkId);
}

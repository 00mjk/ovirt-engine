package org.ovirt.engine.core.common.businessentities;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.core.compat.Guid;

public class LdapGroup extends IVdcQueryable {
    private static final long serialVersionUID = 6717840754119287059L;

    private Guid id;

    private String name;

    private boolean active;

    private String domain;

    private List<String> memberOf;

    private String distinguishedName;

    public LdapGroup() {
        active = true;
        id = Guid.Empty;
        name = "";
        distinguishedName = "";
    }

    public LdapGroup(DbGroup dbGroup) {
        id = dbGroup.getId();
        name = dbGroup.getName();
        domain = dbGroup.getDomain();
        active = dbGroup.isActive();
        distinguishedName = dbGroup.getDistinguishedName();
        memberOf = dbGroup.getMemberOf() != null ? new ArrayList<String>(dbGroup.getMemberOf()) : null;
    }

    public Guid getid() {
        return this.id;
    }

    public void setid(Guid value) {
        this.id = value;
    }

    public String getname() {
        return this.name;
    }

    public void setname(String value) {
        this.name = value;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean value) {
        active = value;
    }

    public LdapGroup(Guid id, String name, String domain) {
        this (id);
        this.name = name;
        this.domain = domain;
    }

    public LdapGroup(Guid id, String name, String domain, String distinguishedName, List<String> memberOf) {
        this(id, name, domain);
        this.distinguishedName = distinguishedName;
        this.setMemberOf(memberOf);
    }

    /**
     * This constructor used only for Inactive groups
     *
     * @param id
     */
    public LdapGroup(Guid id) {
        this.id = id;
        active = false;
    }

    public String getdomain() {
        return domain;
    }

    public void setdomain(String value) {
        domain = value;
    }

    @Override
    public Object getQueryableId() {
        return getid();
    }

    public void setDistinguishedName(String distinguishedName) {
        this.distinguishedName = distinguishedName;
    }

    public String getDistinguishedName() {
        return distinguishedName;
    }

    public void setMemberOf(List<String> memberOf) {
        this.memberOf = memberOf;
    }

    public List<String> getMemberOf() {
        return memberOf;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((distinguishedName == null) ? 0 : distinguishedName.hashCode());
        result = prime * result + ((domain == null) ? 0 : domain.hashCode());
        result = prime * result + ((memberOf == null) ? 0 : memberOf.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (active ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LdapGroup other = (LdapGroup) obj;
        return (ObjectUtils.objectsEqual(id, other.id)
                && ObjectUtils.objectsEqual(distinguishedName, other.distinguishedName)
                && ObjectUtils.objectsEqual(domain, other.domain)
                && ObjectUtils.objectsEqual(memberOf, other.memberOf)
                && ObjectUtils.objectsEqual(name, other.name)
                && active == other.active);
    }
}

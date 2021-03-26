package io.goobi.viewer.model.archives;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;

public class ArchiveEntry {

    private static final Logger logger = LoggerFactory.getLogger(ArchiveEntry.class);

    // parent node
    private ArchiveEntry parentNode;
    // list contains all child elements
    private List<ArchiveEntry> subEntryList = new ArrayList<>();
    // order number of the current element within the current hierarchy
    private Integer orderNumber;
    // hierarchy level
    private Integer hierarchyLevel;
    // c@id
    private String id;
    // display label
    private String label;
    // node is open/closed
    private boolean displayChildren;
    // node is search hit
    private boolean searchHit;
    // node type -  @level
    private String nodeType;
    // display node in a search result
    private boolean displaySearch;
    // true if the validation of all metadata fields was successful
    private boolean valid = true;

    private String descriptionLevel;

    private boolean visible = true;

    private boolean expanded = false;

    private String associatedRecordPi;

    /* 1. metadata for Identity Statement Area */
    //    Reference code(s)
    //    Title
    //    private String unittitle; // did/unittitle
    //    Date(s)
    //    Level of description
    //    Extent and medium of the unit of description (quantity, bulk, or size)
    private List<ArchiveMetadataField> identityStatementAreaList = new ArrayList<>();

    /* 2. Context Area */
    //    Name of creator(s)
    //    Administrative | Biographical history
    //    Archival history
    //    Immediate source of acquisition or transfer
    private List<ArchiveMetadataField> contextAreaList = new ArrayList<>();

    /* 3. Content and Structure Area */
    //    Scope and content
    //    Appraisal, destruction and scheduling information
    //    Accruals
    //    System of arrangement
    private List<ArchiveMetadataField> contentAndStructureAreaAreaList = new ArrayList<>();

    /* 4. Condition of Access and Use Area */
    //    Conditions governing access
    //    Conditions governing reproduction
    //    Language | Scripts of material
    //    Physical characteristics and technical requirements
    //    Finding aids
    private List<ArchiveMetadataField> accessAndUseAreaList = new ArrayList<>();

    /* 5. Allied Materials Area */
    //    Existence and location of originals
    //    Existence and location of copies
    //    Related units of description
    //    Publication note
    private List<ArchiveMetadataField> alliedMaterialsAreaList = new ArrayList<>();

    /* 6. Note Area */
    //    Note
    private List<ArchiveMetadataField> notesAreaList = new ArrayList<>();

    /* 7. Description Control Area */
    //    Archivist's Note
    //    Rules or Conventions
    //    Date(s) of descriptions
    private List<ArchiveMetadataField> descriptionControlAreaList = new ArrayList<>();

    public ArchiveEntry(Integer order, Integer hierarchy) {
        this.orderNumber = order;
        this.hierarchyLevel = hierarchy;
    }

    public void addSubEntry(ArchiveEntry other) {
        subEntryList.add(other);
        other.setParentNode(this);
    }

    public void removeSubEntry(ArchiveEntry other) {
        subEntryList.remove(other);
        reOrderElements();
    }

    public void reOrderElements() {
        int order = 0;
        for (ArchiveEntry entry : subEntryList) {
            entry.setOrderNumber(order++);
        }
    }

    /**
     * 
     * @param ignoreDisplayChildren
     * @return
     */
    public List<ArchiveEntry> getAsFlatList(boolean ignoreDisplayChildren) {
        List<ArchiveEntry> list = new LinkedList<>();
        list.add(this);
        if (displayChildren || ignoreDisplayChildren) {
            if (subEntryList != null && !subEntryList.isEmpty()) {
                for (ArchiveEntry ds : subEntryList) {
                    list.addAll(ds.getAsFlatList(ignoreDisplayChildren));
                    // logger.trace("ID: {}, level {}", ds.getId(), ds.getHierarchy());
                }
            }
        }
        return list;
    }

    public void findAssociatedRecordPi() throws PresentationException {
        if (id == null) {
            associatedRecordPi = "";
            return;
        }
        try {
            SolrDocument doc = DataManager.getInstance()
                    .getSearchIndex()
                    .getFirstDoc("+" + SolrConstants.ARCHIVE_ENTRY_ID + ":" + id, Collections.singletonList(SolrConstants.PI));
            if (doc != null) {
                associatedRecordPi = (String) doc.getFieldValue(SolrConstants.PI);
            }
            if (associatedRecordPi == null) {
                associatedRecordPi = "";
            }
        } catch (SolrException | IndexUnreachableException e) {
            logger.error("Error reading solr database:" + e);
        }
    }

    public boolean isHasChildren() {
        return !subEntryList.isEmpty();
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
        ArchiveEntry other = (ArchiveEntry) obj;
        if (hierarchyLevel == null) {
            if (other.hierarchyLevel != null) {
                return false;
            }
        } else if (!hierarchyLevel.equals(other.hierarchyLevel)) {
            return false;
        }
        if (orderNumber == null) {
            if (other.orderNumber != null) {
                return false;
            }
        } else if (!orderNumber.equals(other.orderNumber)) {
            return false;
        }
        if (parentNode == null && other.parentNode == null) {
            return true;
        }
        if (parentNode == null && other.parentNode != null) {
            return false;
        }
        if (parentNode != null && other.parentNode == null) {
            return false;
        }

        if (other.parentNode != null && !parentNode.getOrderNumber().equals(other.parentNode.getOrderNumber())) {
            return false;
        }
        if (other.parentNode != null && !parentNode.getHierarchyLevel().equals(other.parentNode.getHierarchyLevel())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((hierarchyLevel == null) ? 0 : hierarchyLevel.hashCode());
        result = prime * result + ((orderNumber == null) ? 0 : orderNumber.hashCode());
        result = prime * result + ((parentNode == null) ? 0 : parentNode.getHierarchyLevel().hashCode());
        result = prime * result + ((parentNode == null) ? 0 : parentNode.getOrderNumber().hashCode());
        return result;
    }

    public void updateHierarchy() {
        // root node
        if (parentNode == null) {
            hierarchyLevel = 0;
        } else {
            hierarchyLevel = parentNode.getHierarchyLevel() + 1;
        }

        for (ArchiveEntry child : subEntryList) {
            child.updateHierarchy();
        }
    }

    public void markAsFound(boolean keepChildrenVisible) {
        displaySearch = true;
        searchHit = true;

        if (parentNode != null) {
            ArchiveEntry node = parentNode;
            while (!node.isDisplaySearch()) {
                node.setDisplaySearch(true);
                if (node.parentNode != null) {
                    node = node.parentNode;
                }
            }
        }
        if (keepChildrenVisible) {
            for (ArchiveEntry child : this.subEntryList) {
                child.setDisplaySearch(true, true);
            }
        }
    }

    public void resetFoundList() {
        // logger.trace("resetFoundList: {}", id);
        displaySearch = false;
        searchHit = false;
        if (subEntryList != null) {
            for (ArchiveEntry ds : subEntryList) {
                ds.resetFoundList();
            }
        }
    }

    public List<ArchiveEntry> getSearchList() {
        List<ArchiveEntry> list = new LinkedList<>();
        if (displaySearch) {
            list.add(this);
            if (subEntryList != null) {
                for (ArchiveEntry child : subEntryList) {
                    list.addAll(child.getSearchList());
                }
            }
        }
        return list;
    }

    /**
     * 
     * @param offset
     */
    public void shiftHierarchy(int offset) {
        this.hierarchyLevel += offset;
        if (isHasChildren()) {
            for (ArchiveEntry sub : subEntryList) {
                sub.shiftHierarchy(offset);
            }
        }
    }

    /**
     * Expands and sets visible all ancestors of this node and expands siblings of this node.
     */
    public void expandUp() {
        if (parentNode == null) {
            return;
        }

        parentNode.setVisible(visible);
        parentNode.expand();
        parentNode.expandUp();
    }

    /**
     * Expands this entry and sets all sub-entries visible if their immediate parent is expanded.
     */
    public void expand() {
        // logger.trace("expand: {}", id);
        if (!isHasChildren()) {
            return;
        }

        setExpanded(true);
        setChildrenVisibility(true);
    }

    /**
     * Collapses this entry and hides all sub-entries.
     */
    public void collapse() {
        // logger.trace("collapse: {}", id);
        if (!isHasChildren()) {
            return;
        }

        setExpanded(false);
        setChildrenVisibility(false);
    }

    /**
     * 
     * @param visible
     */
    void setChildrenVisibility(boolean visible) {
        if (!isHasChildren()) {
            return;
        }

        for (ArchiveEntry sub : subEntryList) {
            sub.setVisible(visible);
            if (sub.isExpanded() && sub.isHasChildren()) {
                sub.setChildrenVisibility(visible);
            }
        }
    }

    /**
     * @return the parentNode
     */
    public ArchiveEntry getParentNode() {
        return parentNode;
    }

    /**
     * @param parentNode the parentNode to set
     */
    public void setParentNode(ArchiveEntry parentNode) {
        this.parentNode = parentNode;
    }

    /**
     * @return the subEntryList
     */
    public List<ArchiveEntry> getSubEntryList() {
        return subEntryList;
    }

    /**
     * @param subEntryList the subEntryList to set
     */
    public void setSubEntryList(List<ArchiveEntry> subEntryList) {
        this.subEntryList = subEntryList;
    }

    /**
     * @return the orderNumber
     */
    public Integer getOrderNumber() {
        return orderNumber;
    }

    /**
     * @param orderNumber the orderNumber to set
     */
    public void setOrderNumber(Integer orderNumber) {
        this.orderNumber = orderNumber;
    }

    /**
     * @return the hierarchyLevel
     */
    public Integer getHierarchyLevel() {
        return hierarchyLevel;
    }

    /**
     * @param hierarchyLevel the hierarchyLevel to set
     */
    public void setHierarchyLevel(Integer hierarchyLevel) {
        this.hierarchyLevel = hierarchyLevel;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the displayChildren
     */
    public boolean isDisplayChildren() {
        return displayChildren;
    }

    /**
     * @param displayChildren the displayChildren to set
     */
    public void setDisplayChildren(boolean displayChildren) {
        this.displayChildren = displayChildren;
    }

    /**
     * @return the searchHit
     */
    public boolean isSearchHit() {
        return searchHit;
    }

    /**
     * @param searchHit the searchHit to set
     */
    public void setSearchHit(boolean searchHit) {
        this.searchHit = searchHit;
    }

    /**
     * @return the nodeType
     */
    public String getNodeType() {
        return nodeType;
    }

    /**
     * @param nodeType the nodeType to set
     */
    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    /**
     * @return the displaySearch
     */
    public boolean isDisplaySearch() {
        return displaySearch;
    }

    /**
     * @param displaySearch the displaySearch to set
     */
    public void setDisplaySearch(boolean displaySearch) {
        this.setDisplaySearch(displaySearch, false);
    }

    public void setDisplaySearch(boolean displaySearch, boolean recursive) {
        this.displaySearch = displaySearch;
        if (recursive) {
            for (ArchiveEntry child : this.subEntryList) {
                child.setDisplaySearch(displaySearch, recursive);
            }
        }
    }

    public ArchiveMetadataField getIdentityStatementAreaField(String name) {
        for (ArchiveMetadataField field : identityStatementAreaList) {
            if (field.getLabel().equals(name)) {
                return field;
            }
        }

        return null;
    }

    public List<ArchiveMetadataField> getAllAreaLists() {
        logger.trace("getAllAreaLists ({})", id);
        List<ArchiveMetadataField> ret = new ArrayList<>(getIdentityStatementAreaList().size()
                + getContextAreaList().size()
                + getContentAndStructureAreaAreaList().size()
                + getAccessAndUseAreaList().size()
                + getAlliedMaterialsAreaList().size()
                + getNotesAreaList().size()
                + getDescriptionControlAreaList().size());
        ret.addAll(getIdentityStatementAreaList());
        ret.addAll(getContextAreaList());
        ret.addAll(getContentAndStructureAreaAreaList());
        ret.addAll(getAccessAndUseAreaList());
        ret.addAll(getAlliedMaterialsAreaList());
        ret.addAll(getNotesAreaList());
        ret.addAll(getDescriptionControlAreaList());

        logger.trace("getAllAreaLists END");
        return ret;
    }

    /**
     * @return the identityStatementAreaList
     */
    public List<ArchiveMetadataField> getIdentityStatementAreaList() {
        // logger.trace("getIdentityStatementAreaList ({})", id);
        return identityStatementAreaList;
    }

    /**
     * @param identityStatementAreaList the identityStatementAreaList to set
     */
    public void setIdentityStatementAreaList(List<ArchiveMetadataField> identityStatementAreaList) {
        this.identityStatementAreaList = identityStatementAreaList;
    }

    /**
     * @return the contextAreaList
     */
    public List<ArchiveMetadataField> getContextAreaList() {
        // logger.trace("getContextAreaList ({})", id);
        return contextAreaList;
    }

    /**
     * @param contextAreaList the contextAreaList to set
     */
    public void setContextAreaList(List<ArchiveMetadataField> contextAreaList) {
        this.contextAreaList = contextAreaList;
    }

    /**
     * @return the contentAndStructureAreaAreaList
     */
    public List<ArchiveMetadataField> getContentAndStructureAreaAreaList() {
        // logger.trace("getContentAndStructureAreaAreaList ({})", id);
        return contentAndStructureAreaAreaList;
    }

    /**
     * @param contentAndStructureAreaAreaList the contentAndStructureAreaAreaList to set
     */
    public void setContentAndStructureAreaAreaList(List<ArchiveMetadataField> contentAndStructureAreaAreaList) {
        this.contentAndStructureAreaAreaList = contentAndStructureAreaAreaList;
    }

    /**
     * @return the accessAndUseAreaList
     */
    public List<ArchiveMetadataField> getAccessAndUseAreaList() {
        // logger.trace("getAccessAndUseAreaList ({})", id);
        return accessAndUseAreaList;
    }

    /**
     * @param accessAndUseAreaList the accessAndUseAreaList to set
     */
    public void setAccessAndUseAreaList(List<ArchiveMetadataField> accessAndUseAreaList) {
        this.accessAndUseAreaList = accessAndUseAreaList;
    }

    /**
     * @return the alliedMaterialsAreaList
     */
    public List<ArchiveMetadataField> getAlliedMaterialsAreaList() {
        // logger.trace("getAlliedMaterialsAreaList ({})", id);
        return alliedMaterialsAreaList;
    }

    /**
     * @param alliedMaterialsAreaList the alliedMaterialsAreaList to set
     */
    public void setAlliedMaterialsAreaList(List<ArchiveMetadataField> alliedMaterialsAreaList) {
        this.alliedMaterialsAreaList = alliedMaterialsAreaList;
    }

    /**
     * @return the notesAreaList
     */
    public List<ArchiveMetadataField> getNotesAreaList() {
        // logger.trace("getNotesAreaList ({})", id);
        return notesAreaList;
    }

    /**
     * @param notesAreaList the notesAreaList to set
     */
    public void setNotesAreaList(List<ArchiveMetadataField> notesAreaList) {
        this.notesAreaList = notesAreaList;
    }

    /**
     * @return the descriptionControlAreaList
     */
    public List<ArchiveMetadataField> getDescriptionControlAreaList() {
        // logger.trace("getDescriptionControlAreaList ({})", id);
        return descriptionControlAreaList;
    }

    /**
     * @param descriptionControlAreaList the descriptionControlAreaList to set
     */
    public void setDescriptionControlAreaList(List<ArchiveMetadataField> descriptionControlAreaList) {
        this.descriptionControlAreaList = descriptionControlAreaList;
    }

    /**
     * @return the valid
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * @param valid the valid to set
     */
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /**
     * @return the descriptionLevel
     */
    public String getDescriptionLevel() {
        return descriptionLevel;
    }

    /**
     * @param descriptionLevel the descriptionLevel to set
     */
    public void setDescriptionLevel(String descriptionLevel) {
        this.descriptionLevel = descriptionLevel;
    }

    /**
     * @return the visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @param visible the visible to set
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * @return the expanded
     */
    public boolean isExpanded() {
        return expanded;
    }

    /**
     * @param expanded the expanded to set
     */
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    /**
     * @return the hasChild
     */
    public boolean isHasChild() {
        return !subEntryList.isEmpty();
    }

    /**
     * @return the associatedRecordPi
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public String getAssociatedRecordPi() throws PresentationException, IndexUnreachableException {
        if (associatedRecordPi == null) {
            findAssociatedRecordPi();
        }

        return associatedRecordPi;
    }

    /**
     * Get the parent node hierarchy of this node, optionally including the node itself The list is sorted with hightest hierarchy level first, so the
     * node itself will always be the last element, if included
     * 
     * @param includeSelf
     * @return
     */
    public List<ArchiveEntry> getAncestors(boolean includeSelf) {
        List<ArchiveEntry> ancestors = new ArrayList<>();
        if (includeSelf) {
            ancestors.add(this);
        }
        ArchiveEntry parent = this.parentNode;
        while (parent != null) {
            ancestors.add(parent);
            parent = parent.parentNode;
        }
        Collections.reverse(ancestors);
        return ancestors;

    }

    @Override
    public String toString() {
        return id;
    }
}

package eu.bcvsolutions.idm.core.api.dto.filter;

import eu.bcvsolutions.idm.core.api.dto.IdmTreeNodeDto;

import java.util.UUID;

/**
 * Filter for tree node
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 */
public class TreeNodeFilter extends QuickFilter implements CorrelationFilter {

    private UUID treeTypeId;
    private UUID treeNode;
    private Boolean defaultTreeType; // default tree type wil be used
    private String property;
    private String value;
    /**
     * Tree nodes by tree structure recursively down
     */
    private boolean recursively = true;

    public UUID getTreeTypeId() {
        return treeTypeId;
    }

    public void setTreeTypeId(UUID treeTypeId) {
        this.treeTypeId = treeTypeId;
    }

    public void setTreeNode(UUID treeNode) {
        this.treeNode = treeNode;
    }

    public UUID getTreeNode() {
        return treeNode;
    }

    public Boolean getDefaultTreeType() {
        return defaultTreeType;
    }

    public void setDefaultTreeType(Boolean defaultTreeType) {
        this.defaultTreeType = defaultTreeType;
    }

    public boolean isRecursively() {
        return recursively;
    }

    public void setRecursively(boolean recursively) {
        this.recursively = recursively;
    }

    @Override
    public String getProperty() {
        return property;
    }

    @Override
    public void setProperty(String property) {
        this.property = property;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

}

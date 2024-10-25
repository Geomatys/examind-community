/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.constellation.json.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.constellation.dto.metadata.JsonMetadataConstants;
import static org.constellation.dto.metadata.JsonMetadataConstants.getLastOrdinal;
import static org.constellation.dto.metadata.JsonMetadataConstants.isNumeratedPath;
import org.constellation.dto.metadata.Block;
import org.constellation.dto.metadata.BlockObj;
import org.constellation.dto.metadata.ComponentObj;
import org.constellation.dto.metadata.Field;
import org.constellation.dto.metadata.FieldObj;
import org.constellation.dto.metadata.IBlock;
import static org.constellation.dto.metadata.JsonMetadataConstants.getLastOrdinalIfExist;
import org.constellation.dto.metadata.RootObj;
import org.constellation.dto.metadata.SuperBlock;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class TemplateTree {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.json.metadata");

    private final List<ValueNode> nodes = new ArrayList<>();

    public ValueNode getNodeByPath(String path) {
        for (ValueNode node : nodes) {
            if (node.path.equals(path)) {
                return node;
            }
        }
        return null;
    }

    public ValueNode getNodeByNumeratedPath(String numeratedPath, String blockName) {
        for (ValueNode node : nodes) {
            if (node.getNumeratedPath().equals(numeratedPath)
             && Objects.equals(node.blockName, blockName)) {
                return node;
            }
        }
        return null;
    }

    public ValueNode getNodeByNumeratedPath(String numeratedPath) {
        for (ValueNode node : nodes) {
            if (node.getNumeratedPath().equals(numeratedPath)) {
                return node;
            }
        }
        return null;
    }

    public List<ValueNode> getNodesByPath(String path) {
        final List<ValueNode> results = new ArrayList<>();
        for (ValueNode node : nodes) {
            if (node.path.equals(path)) {
                results.add(node);
            }
        }
        return results;
    }

    public List<ValueNode> getNodesByPathAndType(String path, String type) {
        final List<ValueNode> results = new ArrayList<>();
        for (ValueNode node : nodes) {
            if (node.path.equals(path) && Objects.equals(node.type, type)) {
                results.add(node);
            }
        }
        return results;
    }

    public List<ValueNode> getNodesByBlockName(String blockName) {
        final List<ValueNode> results = new ArrayList<>();
        for (ValueNode node : nodes) {
            if (blockName.equals(node.blockName)) {
                results.add(node);
            }
        }
        return results;
    }

    public List<ValueNode> getNodesForBlock(Block block) {
        final List<ValueNode> results = new ArrayList<>();
        if (block.getPath() != null) {
            for (ValueNode node : nodes) {
                if (block.getName().equals(node.blockName)) {
                    results.add(node);
                }
            }
            return results;
        } else {
            results.add(null);
        }
        return results;
    }


    public ValueNode getRoot() {
        for (ValueNode node : nodes) {
            if (node.parent == null) {
                return node;
            }
        }
        return null;
    }


    /**
     * Duplicate a node only it does not exist
     * @param node
     * @return
     */
    public ValueNode duplicateNode(ValueNode node, int i) {
        String numeratedPath = updateLastOrdinal(node.getNumeratedPath(), i);
        ValueNode exist = getNodeByNumeratedPath(numeratedPath, node.blockName); // issue here
        if (exist == null) {
            int j = i;
            ValueNode n = getNodeByNumeratedPath(numeratedPath);
            while (n != null) {
                j++;
                numeratedPath = updateLastOrdinal(numeratedPath, j);
                ValueNode tmp = getNodeByNumeratedPath(numeratedPath);
                n.updateOrdinal(j);
                n = tmp;
            }
            exist = new ValueNode(node, node.parent, i);
            nodes.add(exist);
            for (ValueNode child : node.children) {
                duplicateNode(child, exist);

            }
        }
        return exist;
    }

    private void duplicateNode(ValueNode node, ValueNode parent) {
        ValueNode newNode = new ValueNode(node, parent, node.ordinal);
        nodes.add(newNode);
        for (ValueNode child : node.children) {
            duplicateNode(child, newNode);
        }
    }

    public List<ValueNode> getNodesByFieldAndParent(String fieldName, ValueNode parent) {
        final List<ValueNode> results = new ArrayList<>();
        for (ValueNode node : nodes) {
            if (node.blockName != null && node.blockName.equals(fieldName) && (parent == null || node.hashParent(parent))) {
                results.add(node);
            }
        }
        return results;
    }

    private String updateLastOrdinal(final String numeratedPath, int ordinal) {
        int i = numeratedPath.lastIndexOf('[');
        if (i != -1) {
            return numeratedPath.substring(0, i + 1) + ordinal +"]";
        }
        throw new IllegalArgumentException(numeratedPath + " does not contain numerated value");
    }

    private void addNode(ValueNode node, ValueNode ancestor, final RootObj template, String numPath) {
        nodes.add(node);

        // for a new Node to add, we create all the missing parent nodes
        ValueNode child = node;
        String path     = node.path;
        while (path.indexOf('.') != -1) {
            path    = path.substring(0, path.lastIndexOf('.'));
            numPath = numPath.substring(0, numPath.lastIndexOf('.'));
            int i   = getLastOrdinal(numPath);

            List<ValueNode> parents;
            if (isNumeratedPath(numPath)) {
                parents = new ArrayList<>();
                ValueNode v = getNodeByNumeratedPath(numPath);
                if (v != null) {
                    parents.add(v);
                }
            } else {
                parents = getNodesByPath(path);
            }
            if (parents.isEmpty()) {
                ValueNode parent = new ValueNode(path, template.getTypeForPath(path), i, null, null, false);
                nodes.add(parent);
                parent.addChild(child);
                child = parent;
            } else {
                boolean found = false;
                for (ValueNode parent : parents) {
                    if (ancestor == null || parent.hashParent(ancestor) || parent.simpleEquals(ancestor)) {
                        parent.addChild(child);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    ValueNode parent = new ValueNode(path, template.getTypeForPath(path), i, null, null,false);
                    nodes.add(parent);
                    parent.addChild(child);
                    child = parent;
                } else {
                    break;
                }
            }
        }

    }

    private void moveFollowingNumeratedPath(String path, int ordinal) {
        path = JsonMetadataConstants.cleanNumeratedPath(path);
        List<ValueNode> toUpdate = getNodesByPath(path);
        for (ValueNode n : toUpdate) {
            if (n.ordinal >= ordinal) {
                n.updateOrdinal(n.ordinal++);
            }
        }
    }

    public List<ValueNode> getOrphanNode() {
        List<ValueNode> orphans = new ArrayList<>();
        ValueNode root = getRoot();
        for (ValueNode node : nodes) {
            if (!node.hashParent(root)) {
                orphans.add(node);
            }
        }
        return orphans;
    }

    public static TemplateTree getTreeFromRootObj(RootObj template) {
        final TemplateTree tree = new TemplateTree();
        for (SuperBlock sb : template.getSuperBlocks()) {
            final Map<String, Integer> blockPathOrdinal = new HashMap<>();
            for (BlockObj block : sb.getChildren()) {
                updateTreeFromBlock(block, tree, template, blockPathOrdinal);
            }
        }

        return tree;
    }

    public static void updateTreeFromBlock(BlockObj blockO, TemplateTree tree, final RootObj template, Map<String, Integer> blockPathOrdinal) {
        final Block block = blockO.getBlock();

        // Multiple Block
        ValueNode ancestor = null;
        if (block.getPath() != null) {
            int ordinal = updateOrdinal(blockPathOrdinal, block.getPath());
            int originalOrdinal = getLastOrdinalIfExist(block.getPath());

            ancestor = new ValueNode(block, ordinal);
            if (block.getPath().endsWith("+") || (originalOrdinal != -1 && ordinal != originalOrdinal)) {
                block.updatePath(ordinal);
                template.moveFollowingNumeratedPath(block, ordinal);
            }
            tree.addNode(ancestor, null, template, block.getPath());
        }

        // Fields
        final Map<String, Integer> fieldPathOrdinal = new HashMap<>();
        for (ComponentObj child : block.getChildren()) {
            if (child instanceof FieldObj) {
                if (((FieldObj) child).getField().isOutOfBlock()) {
                    updateTreeFromField((FieldObj)child, tree, null, template, fieldPathOrdinal);
                } else {
                    updateTreeFromField((FieldObj)child, tree, ancestor, template, fieldPathOrdinal);
                }
            } else {
                updateTreeFromBlock((BlockObj)child, tree, template, blockPathOrdinal);
            }
        }
    }

    public static void updateTreeFromField(FieldObj fieldObj, TemplateTree tree, final ValueNode ancestor, final RootObj template, Map<String, Integer> fieldPathOrdinal) {
        final Field field = fieldObj.getField();
        int fieldOrdinal = updateOrdinal(fieldPathOrdinal, field.getPath());
        final ValueNode node = new ValueNode(field, fieldOrdinal);
        tree.addNode(node, ancestor, template, field.getPath());
    }

    private static int updateOrdinal(Map<String, Integer> pathOrdinal, String path) {
        path = JsonMetadataConstants.removeLastNumeratedPathPart(path);
        int ordinal = 0;
        if (pathOrdinal.containsKey(path)) {
            ordinal = pathOrdinal.get(path) + 1;
        }
        pathOrdinal.put(path, ordinal);
        return ordinal;
    }

    public static RootObj getRootObjFromTree(final RootObj rootobj, final TemplateTree tree, final boolean prune) {
        final RootObj result = new RootObj(rootobj);

        for (SuperBlock sb : result.getSuperBlocks()) {
            final List<BlockObj> children = new ArrayList<>(sb.getChildren());
            int blockCount = 0;
            for (BlockObj block : children) {
                blockCount = updateRootObjFromTree(block, sb, tree, blockCount);
                blockCount++;
                if (prune && block.getBlock().childrenEmpty()) {
                    sb.removeBlock(block);
                    blockCount--;
                }
            }
            if (sb.childrenEmpty()) {
                result.getRoot().remove(sb);
            }
        }

        return result;
    }

    private static int updateRootObjFromTree(final BlockObj blockObj, final IBlock owner, final TemplateTree tree, int blockCount) {
        Block block = blockObj.getBlock();
        final Block origBlock = new Block(block);
        final List<ValueNode> blockNodes = tree.getNodesForBlock(block);

        if (blockNodes.isEmpty()) {
            owner.removeBlock(blockObj);
            blockCount--;
        }

        for (int i = 0; i < blockNodes.size(); i++) {
            final ValueNode node = blockNodes.get(i);
            if (i > 0) {
                block = owner.addBlock(blockCount + 1, new Block(origBlock));
                blockCount++;
            }
            if (node != null) {
                block.setPath(node.getNumeratedPath());
            }

            final List<ComponentObj> blockChildren = new ArrayList<>(block.getChildren());
            int fieldCount = 0;
            for (ComponentObj child : blockChildren) {
                if (child instanceof FieldObj) {
                    fieldCount = updateRootObjFromTree((FieldObj) child, block, tree, node, fieldCount);
                } else {
                    fieldCount = updateRootObjFromTree((BlockObj) child, block, tree, fieldCount);
                }
                fieldCount++;
            }
        }
        return blockCount;
    }

    private static int updateRootObjFromTree(final FieldObj fieldObj, final Block owner, final TemplateTree tree, ValueNode parent, int fieldCount) {
        Field field = fieldObj.getField();

        // if field is out of block, its not a child of the block parent node
        if (field.isOutOfBlock()) {
            parent = null;
        }
        final List<ValueNode> fieldNodes = tree.getNodesByFieldAndParent(field.getName(), parent);

        if (fieldNodes.isEmpty()) {
            owner.removeField(fieldObj);
        }

        for (int j = 0; j < fieldNodes.size(); j++) {
            final ValueNode childNode = fieldNodes.get(j);
            if (j > 0) {
                if (field.getMultiplicity() > 1) {
                    field = owner.addField(fieldCount + 1, new Field(field));
                    fieldCount++;
                } else {
                    LOGGER.log(Level.FINE, "field value excluded for multiplicity purpose:{0}", field.getPath());
                    continue;
                }
            }
            field.setPath(childNode.getNumeratedPath());
            field.setValue(childNode.value);
        }
        return fieldCount;
    }

    public static void pruneTree(TemplateTree tree, final ValueNode node) {
        List<ValueNode> toRemove = new ArrayList<>();
        for (ValueNode child : node.children) {
            pruneTree(tree, child);
            if (child.isField()) {
                if (child.value == null || child.value.isEmpty()) {
                    toRemove.add(child);
                }
            }
        }
        if (!toRemove.isEmpty()) {
            node.children.removeAll(toRemove);
            tree.nodes.removeAll(toRemove);
        }
    }
}

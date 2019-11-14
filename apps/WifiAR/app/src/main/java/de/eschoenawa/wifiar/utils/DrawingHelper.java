package de.eschoenawa.wifiar.utils;

import android.support.annotation.Nullable;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;

import static de.eschoenawa.wifiar.common.Constants.INDICATOR_HEIGHT;

/**
 * This Utility-class provides a few helper methods for drawing in ARCore.
 *
 * @author Emil Schoenawa
 */
public class DrawingHelper {
    /**
     * Generates a line at the first Node towards the second one.
     *
     * @param node1    The Node to draw the line from (do not use a Node that already has a Renderable as it will be overwritten)
     * @param node2    The Node to draw the line towards (this Node may already have a renderable)
     * @param material The Material of the line
     */
    public static void drawLineBetweenNodes(Node node1, Node node2, Material material) {
        drawLineAtNodeToPoint(node1, node2.getWorldPosition(), material);
    }

    /**
     * Generates a line at the Node towards the point.
     *
     * @param node     The Node to draw the line from (do not use a Node that already has a Renderable as it will be overwritten)
     * @param point    The point to draw the line towards
     * @param material The Material of the line
     */
    public static void drawLineAtNodeToPoint(Node node, Vector3 point, Material material) {
        Vector3 lineVector = Vector3.subtract(node.getWorldPosition(), point);
        Renderable line = ShapeFactory.makeCube(new Vector3(lineVector.length(), 0.00f, 0.01f), new Vector3(lineVector.length() / 2f, 0.0f, 0.0f), material);
        node.setRenderable(line);
        Vector3 defaultLineDirectionVector = new Vector3(-1, 0, 0);
        node.setWorldRotation(Quaternion.rotationBetweenVectors(defaultLineDirectionVector, lineVector));
    }

    /**
     * Attaches a pillar (marker) to the Anchor in the given {@link Scene} using the given {@link Material}
     *
     * @param anchor         The Anchor to attach the pillar to
     * @param pillarMaterial The {@link Material} of the pillar
     * @param scene          The {@link Scene} to place the pillar in
     * @return The node created to hold the pillar renderable
     */
    public static Node attachPillarToAnchorIfPossible(@Nullable Anchor anchor, Material pillarMaterial, Scene scene) {
        if (anchor != null) {
            AnchorNode anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(scene);
            Renderable pillar = ShapeFactory.makeCube(new Vector3(0.01f, INDICATOR_HEIGHT, 0.01f), new Vector3(0.0f, INDICATOR_HEIGHT / 2, 0.0f), pillarMaterial);
            Node n = new Node();
            n.setParent(anchorNode);
            n.setRenderable(pillar);
            return n;
        }
        return null;
    }

    /**
     * Attaches a pillar (marker) to the Anchor in the given {@link Scene} using the given {@link Material}
     *
     * @param anchor               The Anchor to attach the pillar to
     * @param pillarMaterial       The {@link Material} of the pillar
     * @param desiredWorldPosition The position where the pillar should be placed, use {@code null}
     *                             or {@link #attachPillarToAnchorIfPossible(Anchor, Material, Scene)}
     *                             to place it at the anchor position
     * @param scene                The {@link Scene} to place the pillar in
     * @return The node created to hold the pillar renderable
     */
    public static Node attachPillarToAnchorIfPossible(@Nullable Anchor anchor, Material pillarMaterial, Vector3 desiredWorldPosition, Scene scene) {
        Node attachedNode = attachPillarToAnchorIfPossible(anchor, pillarMaterial, scene);
        if (attachedNode != null && desiredWorldPosition != null) {
            attachedNode.setWorldPosition(desiredWorldPosition);
        }
        return attachedNode;
    }

    /**
     * Moves a {@link Node} to the same height as another {@link Node}
     *
     * @param nodeToCorrect   The {@link Node} to move
     * @param nodeAtLocalZero The {@link Node} with the correct height
     * @return The height-corrected {@link Node}
     */
    public static Node correctToSameHeight(Node nodeToCorrect, Node nodeAtLocalZero) {
        float zeroY = nodeAtLocalZero.getLocalPosition().y;
        Vector3 position = nodeToCorrect.getLocalPosition();
        position.y = zeroY;
        nodeToCorrect.setLocalPosition(position);
        return nodeToCorrect;
    }
}

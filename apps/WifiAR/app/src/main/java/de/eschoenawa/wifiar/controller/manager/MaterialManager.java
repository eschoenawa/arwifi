package de.eschoenawa.wifiar.controller.manager;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;

/**
 * This class manages the materials required for drawing the markers in AR.
 *
 * @author Emil Schoenawa
 */
public class MaterialManager {
    private Context context;
    private Material targetIndicatorMaterial;
    private Material areaIndicatorMaterial;
    private Material measurementIndicatorMaterial;
    private Material areaLineMaterial;

    /**
     * Starts the generation of the materials and creates a new manager.
     * @param context A context for creating the material
     */
    public MaterialManager(@NonNull Context context) {
        // Generate materials
        this.context = context;
        generateTargetIndicatorMaterial();
        generateAreaIndicatorMaterial();
        generateMeasurementIndicatorMaterial();
        generateAreaLineMaterial();
    }

    public Material getTargetIndicatorMaterial() {
        return targetIndicatorMaterial;
    }

    public Material getAreaIndicatorMaterial() {
        return areaIndicatorMaterial;
    }

    public Material getMeasurementIndicatorMaterial() {
        return measurementIndicatorMaterial;
    }

    public Material getAreaLineMaterial() {
        return areaLineMaterial;
    }

    // Methods for generating materials
    private void generateTargetIndicatorMaterial() {
        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.RED))
                .thenAccept(
                        material -> this.targetIndicatorMaterial = material);
    }

    private void generateAreaIndicatorMaterial() {
        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.BLUE))
                .thenAccept(
                        material -> this.areaIndicatorMaterial = material);
    }

    private void generateMeasurementIndicatorMaterial() {
        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.GREEN))
                .thenAccept(
                        material -> this.measurementIndicatorMaterial = material);
    }

    private void generateAreaLineMaterial() {
        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.YELLOW))
                .thenAccept(
                        material -> this.areaLineMaterial = material);
    }
}

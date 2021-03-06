package volvis;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import gui.RaycastRendererPanel;
import gui.TransferFunction2DEditor;
import gui.TransferFunctionEditor;
import java.awt.image.BufferedImage;

import util.TFChangeListener;
import util.VectorMath;
import volume.GradientVolume;
import volume.Volume;
import volume.VoxelGradient;

/**
 * Raycast Renderer.
 *
 * @author Michel Westenberg
 * @author Anna Vilanova
 * @author Nicola Pezzotti
 * @author Humberto Garcia
 */
public class RaycastRenderer extends Renderer implements TFChangeListener {

    /**
     * Volume that is loaded and visualized.
     */
    private Volume volume = null;

    /**
     * Rendered image.
     */
    private BufferedImage image;

    /**
     * Gradient information of the loaded volume.
     */
    private GradientVolume gradients = null;

    /**
     * Reference to the GUI panel.
     */
    RaycastRendererPanel panelFront;

    /**
     * Transfer Function.
     */
    TransferFunction tFuncFront;

    /**
     * Reference to the GUI transfer function editor.
     */
    TransferFunctionEditor tfEditor;

    /**
     * Transfer Function 2D.
     */
    TransferFunction2D tFunc2DFront;

    /**
     * Reference to the GUI 2D transfer function editor.
     */
    TransferFunction2DEditor tfEditor2DFront;

    /**
     * Mode of our raycast. See {@link RaycastMode}
     */
    private RaycastMode modeFront;

    /**
     * Whether we are in cutting plane mode or not.
     */
    private boolean cuttingPlaneMode = false;

    /**
     * Whether we are in shading mode or not.
     */
    private boolean shadingMode = false;

    /**
     * Iso value to use in Isosurface rendering.
     */
    private float isoValueFront = 95f;

    /**
     * Color used for the isosurface rendering.
     */
    private TFColor isoColorFront;

    // Below cutting plane specific attributes
    /**
     * Cutting plane normal vector.
     */
    private final double[] planeNorm = new double[]{0d, 0d, 1d};

    /**
     * Cutting plane point.
     */
    private final double[] planePoint = new double[]{0d, 0d, 0d};

    /**
     * Back mode of our raycast for cutting plane.
     */
    private RaycastMode modeBack;

    /**
     * Iso value to use in Isosurface rendering for cutting plane.
     */
    private float isoValueBack = 95f;

    /**
     * Color used for the isosurface rendering for cutting plane.
     */
    private TFColor isoColorBack;

    /**
     * Transfer Function for cutting plane.
     */
    TransferFunction tFuncBack;

    /**
     * Reference to the GUI transfer function editor for cutting plane.
     */
    TransferFunctionEditor tfEditorBack;

    /**
     * Transfer Function 2D for cutting plane.
     */
    TransferFunction2D tFunc2DBack;

    /**
     * Reference to the GUI 2D transfer function editor for cutting plane.
     */
    TransferFunction2DEditor tfEditor2DBack;

    /**
     * Constant Zero gradient.
     */
    private final static VoxelGradient ZERO_GRADIENT = new VoxelGradient();

    /**
     * Gets the corresponding voxel using Nearest Neighbors.
     *
     * @param coord Pixel coordinate in 3D space of the voxel we want to get.
     * @return The voxel value.
     */
    private short getVoxel(double[] coord) {
        // Get coordinates
        double dx = coord[0], dy = coord[1], dz = coord[2];

        // Verify they are inside the volume
        if (dx < 0 || dx >= volume.getDimX() || dy < 0 || dy >= volume.getDimY()
                || dz < 0 || dz >= volume.getDimZ()) {

            // If not, jus return 0
            return 0;
        }

        // Get the closest x, y, z to dx, dy, dz that are integers
        // This is important as our data is discrete (not continuous)
        int x = (int) Math.floor(dx);
        int y = (int) Math.floor(dy);
        int z = (int) Math.floor(dz);

        // Finally, get the voxel from the Volume for the corresponding coordinates
        return volume.getVoxel(x, y, z);
    }

    /**
     * Gets the corresponding voxel using Tri-linear Interpolation.
     *
     * @param coord Pixel coordinate in 3D space of the voxel we want to get.
     * @return The voxel value.
     */
    private short getVoxelTrilinear(double[] coord) {
        // TODO 1: Implement Tri-Linear interpolation and use it in your code
        // instead of getVoxel().
        
        // Get coordinates
        double dx = coord[0], dy = coord[1], dz = coord[2];
        
        //First we acquire the ceilings and floors, later used to index the correct
        //voxels.
        
        int xFloor = (int) Math.floor(dx);
        int yFloor = (int) Math.floor(dy);
        int zFloor = (int) Math.floor(dz);
        int xCeil  = (int) Math.ceil(dx);
        int yCeil  = (int) Math.ceil(dy);
        int zCeil  = (int) Math.ceil(dz); 
        
        //We do the same checks that are conducted in the getVoxel that floors
        //coordinates. But now a little bit altered so we can use ceiling commands
        
        // Verify they are inside the volume
        if (xFloor < 0 || xCeil >= volume.getDimX() || yFloor < 0 || yCeil >= volume.getDimY()
                || zFloor < 0 || zCeil >= volume.getDimZ()) {

            // If not, jus return 0
            return 0;
        }

        //To do tri-linear interpolation we have to acquire the value of the 8 surrounding
        //actual voxels and linear interpolate over every axis. So first we take the
        //8 values and name them the same as in sheet 2 of the sheet set 2-spatial.
        
        
        
        short x0 = volume.getVoxel(xFloor,yFloor,zFloor);
        short x1 = volume.getVoxel(xCeil,yFloor,zFloor);
        short x2 = volume.getVoxel(xFloor,yCeil,zFloor);
        short x3 = volume.getVoxel(xCeil,yCeil,zFloor);
        short x4 = volume.getVoxel(xFloor,yFloor,zCeil);
        short x5 = volume.getVoxel(xCeil,yFloor,zCeil);
        short x6 = volume.getVoxel(xFloor,yCeil,zCeil);
        short x7 = volume.getVoxel(xCeil,yCeil,zCeil);
        
        //Now we acquire alpha for x, beta for y and gamma for the z acces which is a
        //The distance from x0 for every axes
        
        double alpha = dx - Math.floor(dx);
        double beta  = dy - Math.floor(dy);
        double gamma = dz - Math.floor(dz);
        
        //now we just copy the formula from the sheet. note that shorts are widened to
        //accommodate the double multiplacations, but we can always cast back afterwards
        
        
        double S = (1-alpha)*(1-beta)*(1-gamma)*x0 + alpha*(1-beta)*(1-gamma)*x1
                  +(1-alpha)*beta*(1-gamma)*x2 + alpha*beta*(1-gamma)*x3
                  +(1-alpha)*(1-beta)*gamma*x4 + alpha*(1-beta)*gamma*x5
                  +(1-alpha)*beta*gamma*x6 + alpha*beta*gamma*x7;
        
        return (short) S;
        
    }

    /**
     * Gets the corresponding VoxelGradient using Nearest Neighbors.
     *
     * @param coord Pixel coordinate in 3D space of the voxel we want to get.
     * @return The voxel gradient.
     */
    private VoxelGradient getGradient(double[] coord) {
        // Get the coordinates
        double dx = coord[0], dy = coord[1], dz = coord[2];

        // Verify they are inside the volume gradient
        if (dx < 0 || dx > (gradients.getDimX() - 2) || dy < 0 || dy > (gradients.getDimY() - 2)
                || dz < 0 || dz > (gradients.getDimZ() - 2)) {

            // If not, just return a zero gradient
            return ZERO_GRADIENT;
        }

        // Get the closest x, y, z to dx, dy, dz that are integers
        // This is important as our data is discrete (not continuous)
        int x = (int) Math.round(dx);
        int y = (int) Math.round(dy);
        int z = (int) Math.round(dz);

        // Finally, get the gradient from GradientVolume for the corresponding coordinates
        return gradients.getGradient(x, y, z);
    }

    /**
     * Gets the corresponding VoxelGradient using Tri-linear interpolation.
     *
     * @param coord Pixel coordinate in 3D space of the voxel we want to get.
     * @return The voxel gradient.
     */
    private VoxelGradient getGradientTrilinear(double[] coord) {
        // TODO 6: Implement Tri-linear interpolation for gradients
        // This is a near copy of the getVoxelTrilinear but now computed on gradients
        
        // Get the coordinates, subtract one so we align with volume coordinate values
        // however this is ignored in other gradient calculation so we ignore aswell here
        double dx = coord[0], dy = coord[1], dz = coord[2];
        
        
        //First we acquire the ceilings and floors, later used to index the correct
        //voxels.
        
        int xFloor = (int) Math.floor(dx);
        int yFloor = (int) Math.floor(dy);
        int zFloor = (int) Math.floor(dz);
        int xCeil  = (int) Math.ceil(dx);
        int yCeil  = (int) Math.ceil(dy);
        int zCeil  = (int) Math.ceil(dz); 

        // Verify they are inside the volume gradient
        if (xFloor < 0 || xCeil > (gradients.getDimX() - 2) || yFloor < 0 || yCeil > (gradients.getDimY() - 2)
                || zFloor < 0 || zCeil > (gradients.getDimZ() - 2)) {

            // If not, just return a zero gradient
            return ZERO_GRADIENT;
        }

        //To do tri-linear interpolation we have to acquire the value of the 8 surrounding
        //actual voxels and linear interpolate over every axis. So first we take the
        //8 values and name them the same as in sheet 2 of the sheet set 2-spatial.
        VoxelGradient x0 = gradients.getGradient(xFloor,yFloor,zFloor);
        VoxelGradient x1 = gradients.getGradient(xCeil,yFloor,zFloor);
        VoxelGradient x2 = gradients.getGradient(xFloor,yCeil,zFloor);
        VoxelGradient x3 = gradients.getGradient(xCeil,yCeil,zFloor);
        VoxelGradient x4 = gradients.getGradient(xFloor,yFloor,zCeil);
        VoxelGradient x5 = gradients.getGradient(xCeil,yFloor,zCeil);
        VoxelGradient x6 = gradients.getGradient(xFloor,yCeil,zCeil);
        VoxelGradient x7 = gradients.getGradient(xCeil,yCeil,zCeil);
        
        //Now we acquire alpha for x, beta for y and gamma for the z acces which is a
        //The distance from x0 for every axes
        
        float alpha = (float) (dx - Math.floor(dx));
        float beta  = (float) (dy - Math.floor(dy));
        float gamma = (float) (dz - Math.floor(dz));
        
        //now we just copy the formula from the sheet. note that shorts are widened to
        //accommodate the double multiplacations, but we can always cast back afterwards
        
        
        float xGradient = (1-alpha)*(1-beta)*(1-gamma)*x0.x + alpha*(1-beta)*(1-gamma)*x1.x
                            +(1-alpha)*beta*(1-gamma)*x2.x + alpha*beta*(1-gamma)*x3.x
                            +(1-alpha)*(1-beta)*gamma*x4.x + alpha*(1-beta)*gamma*x5.x
                            +(1-alpha)*beta*gamma*x6.x + alpha*beta*gamma*x7.x;
        
        float yGradient = (1-alpha)*(1-beta)*(1-gamma)*x0.y + alpha*(1-beta)*(1-gamma)*x1.y
                            +(1-alpha)*beta*(1-gamma)*x2.y + alpha*beta*(1-gamma)*x3.y
                            +(1-alpha)*(1-beta)*gamma*x4.y + alpha*(1-beta)*gamma*x5.y
                            +(1-alpha)*beta*gamma*x6.y + alpha*beta*gamma*x7.y;
        
        float zGradient = (1-alpha)*(1-beta)*(1-gamma)*x0.z + alpha*(1-beta)*(1-gamma)*x1.z
                            +(1-alpha)*beta*(1-gamma)*x2.z + alpha*beta*(1-gamma)*x3.z
                            +(1-alpha)*(1-beta)*gamma*x4.z + alpha*(1-beta)*gamma*x5.z
                            +(1-alpha)*beta*gamma*x6.z + alpha*beta*gamma*x7.z;
        
        return new VoxelGradient(xGradient,yGradient,zGradient);
    }

    /**
     * Updates {@link #image} attribute (result of rendering) using the slicing
     * technique.
     *
     * @param viewMatrix OpenGL View matrix {
     * @see
     * <a href="www.songho.ca/opengl/gl_transform.html#modelview">link</a>}.
     */
    private void slicer(double[] viewMatrix) {

        // Clear the image
        resetImage();

        // vector uVec and vVec define a plane through the origin,
        // perpendicular to the view vector viewVec which is going from the view point towards the object
        // uVec contains the up vector of the camera in world coordinates (image vertical)
        // vVec contains the horizontal vector in world coordinates (image horizontal)
        double[] viewVec = new double[3];
        double[] uVec = new double[3];
        double[] vVec = new double[3];
        VectorMath.setVector(viewVec, viewMatrix[2], viewMatrix[6], viewMatrix[10]);
        VectorMath.setVector(uVec, viewMatrix[0], viewMatrix[4], viewMatrix[8]);
        VectorMath.setVector(vVec, viewMatrix[1], viewMatrix[5], viewMatrix[9]);

        // We get the size of the image/texture we will be puting the result of the 
        // volume rendering operation.
        int imageW = image.getWidth();
        int imageH = image.getHeight();

        int[] imageCenter = new int[2];
        // Center of the image/texture 
        imageCenter[0] = imageW / 2;
        imageCenter[1] = imageH / 2;

        double[] pixelCoord = new double[3];
        double[] volumeCenter = new double[3];
        VectorMath.setVector(volumeCenter, volume.getDimX() / 2, volume.getDimY() / 2, volume.getDimZ() / 2);

        // sample on a plane through the origin of the volume data
        double max = volume.getMaximum();

        TFColor pixelColor = new TFColor();
        // Auxiliar color
        TFColor colorAux;

        for (int j = imageCenter[1] - imageH / 2; j < imageCenter[1] + imageH / 2; j++) {
            for (int i = imageCenter[0] - imageW / 2; i < imageCenter[0] + imageW / 2; i++) {
                // computes the pixelCoord which contains the 3D coordinates of the pixels (i,j)
                computePixelCoordinatesFloat(pixelCoord, volumeCenter, uVec, vVec, i, j);

                //int val = getVoxel(pixelCoord);
                //NOTE: you have to implement this function to get the tri-linear interpolation
                int val = getVoxelTrilinear(pixelCoord);

                // Map the intensity to a grey value by linear scaling
                pixelColor.r = val / max;
                pixelColor.g = pixelColor.r;
                pixelColor.b = pixelColor.r;
                pixelColor.a = val > 0 ? 1.0 : 0.0;  // this makes intensity 0 completely transparent and the rest opaque
                // Alternatively, apply the transfer function to obtain a color
                // pixelColor = tFuncFront.getColor(val);

                //BufferedImage/image/texture expects a pixel color packed as ARGB in an int
                //use the function computeImageColor to convert your double color in the range 0-1 to the format need by the image
                int packedPixelColor = computePackedPixelColor(pixelColor.r, pixelColor.g, pixelColor.b, pixelColor.a);
                image.setRGB(i, j, packedPixelColor);
            }
        }
    }

    /**
     * Do NOT modify this function.
     *
     * Updates {@link #image} attribute (result of rendering) using MIP
     * raycasting. It returns the color assigned to a ray/pixel given its
     * starting and ending points, and the direction of the ray.
     *
     * @param entryPoint Starting point of the ray.
     * @param exitPoint Last point of the ray.
     * @param rayVector Direction of the ray.
     * @param sampleStep Sample step of the ray.
     * @return Color assigned to a ray/pixel.
     */
    private int traceRayMIP(double[] entryPoint, double[] exitPoint, double[] rayVector, double sampleStep) {
        //compute the increment and the number of samples
        double[] increments = new double[3];
        VectorMath.setVector(increments, rayVector[0] * sampleStep, rayVector[1] * sampleStep, rayVector[2] * sampleStep);

        // Compute the number of times we need to sample
        double distance = VectorMath.distance(entryPoint, exitPoint);
        int nrSamples = 1 + (int) Math.floor(VectorMath.distance(entryPoint, exitPoint) / sampleStep);

        //the current position is initialized as the entry point
        double[] currentPos = new double[3];
        VectorMath.setVector(currentPos, entryPoint[0], entryPoint[1], entryPoint[2]);

        double maximum = 0;
        do {
            double value = getVoxelTrilinear(currentPos) / 255.;
            if (value > maximum) {
                maximum = value;
            }
            for (int i = 0; i < 3; i++) {
                currentPos[i] += increments[i];
            }
            nrSamples--;
        } while (nrSamples > 0);

        double alpha;
        double r, g, b;
        if (maximum > 0.0) { // if the maximum = 0 make the voxel transparent
            alpha = 1.0;
        } else {
            alpha = 0.0;
        }
        r = g = b = maximum;
        int color = computePackedPixelColor(r, g, b, alpha);
        return color;
    }

    /**
     *
     * Updates {@link #image} attribute (result of rendering) using the
     * Isosurface raycasting. It returns the color assigned to a ray/pixel given
     * its starting and ending points, and the direction of the ray.
     *
     * @param entryPoint Starting point of the ray.
     * @param exitPoint Last point of the ray.
     * @param rayVector Direction of the ray.
     * @param sampleStep Sample step of the ray.
     * @return Color assigned to a ray/pixel.
     */
    private int traceRayIso(double[] entryPoint, double[] exitPoint, double[] rayVector, double sampleStep, boolean isFront) {

        double[] lightVector = new double[3];
        //We define the light vector as directed toward the view point (which is the source of the light)
        // another light vector would be possible
        VectorMath.setVector(lightVector, rayVector[0], rayVector[1], rayVector[2]);

        // TODO 3: Implement isosurface rendering.
        //Initialization of the colors as floating point values
        double r, g, b;
        r = g = b = 0.0;
        double alpha = 0.0;
        
        
        //compute the increment and the number of samples
        double[] increments = new double[3];
        VectorMath.setVector(increments, rayVector[0] * sampleStep, rayVector[1] * sampleStep, rayVector[2] * sampleStep);

        // Compute the number of times we need to sample
        //double distance = VectorMath.distance(entryPoint, exitPoint); // is not used
        int nrSamples = 1 + (int) Math.floor(VectorMath.distance(entryPoint, exitPoint) / sampleStep);

        //the current position is initialized as the closest point and we work back to the furthest point
        double[] currentPos = new double[3];
        VectorMath.setVector(currentPos, entryPoint[0], entryPoint[1], entryPoint[2]);
        
        //Now we want to detect a surface so we first see if we are below or above it.
        //If we are below it we are looking for a rising edge
        
        short firstValue = getVoxelTrilinear(currentPos);
        boolean surfaceFound = false;
        
        //back or front
        
        float isoValue;
        TFColor isoColor;
        
        if(isFront)
        {
            isoValue = isoValueFront;
            isoColor = isoColorFront;
        }
        else
        {
            isoValue = isoValueBack;
            isoColor = isoColorBack;
        }
               
        
        //We can detect the transion from below to above. But of course the first all values
        //can have the isovalue, in which case we would not detect it, so we check seperately.
        if(firstValue == isoValue)
        {
            surfaceFound = true;
        }
        else
        {
            boolean potentialRisingEdge = firstValue < isoValue;
        
        
            //keep going untill we found surface or we have seen all samples
            while(nrSamples > 1 && !surfaceFound) {  
                
                //set step
                for (int i = 0; i < 3; i++) {
                    currentPos[i] += increments[i];
                }
                nrSamples--;
                
                //see if we can see an transion, works only when going through the surface
                if(potentialRisingEdge)
                {
                    surfaceFound = getVoxelTrilinear(currentPos) >= isoValue;

                }
                else
                {
                    surfaceFound = getVoxelTrilinear(currentPos) < isoValue;
                }
                
            }
        }
        
        if(surfaceFound)
        {
            // isoColorFront contains the isosurface color from the GUI
            
            TFColor color = isoColor;
            
            //lets apply shading on this color
            
            if(shadingMode)
            {
                VoxelGradient gradient = getGradientTrilinear(currentPos);
            
                color = computePhongShading(isoColor,gradient,lightVector,rayVector);
            }
        
            
            r = color.r;
            g = color.g;
            b = color.b;
            
            alpha = 1.0;  
        }
              
        //computes the color, note that pixels wihtout surface have a alpha of 0
        int color = computePackedPixelColor(r, g, b, alpha);
        return color;
    }

    /**
     *
     * Updates {@link #image} attribute (result of rendering) using the
     * compositing/accumulated raycasting. It returns the color assigned to a
     * ray/pixel given its starting and ending points, and the direction of the
     * ray.
     *
     * Ray must be sampled with a distance defined by sampleStep.
     *
     * @param entryPoint Starting point of the ray.
     * @param exitPoint Last point of the ray.
     * @param rayVector Direction of the ray.
     * @param sampleStep Sample step of the ray.
     * @return Color assigned to a ray/pixel.
     */
    private int traceRayComposite(double[] entryPoint, double[] exitPoint, double[] rayVector, double sampleStep, boolean isFront) {
        double[] lightVector = new double[3];

        //the light vector is directed toward the view point (which is the source of the light)
        // another light vector would be possible 
        VectorMath.setVector(lightVector, rayVector[0], rayVector[1], rayVector[2]);

        //Initialization of the colors as floating point values
        double opacity = 0;

        TFColor voxel_color = new TFColor();
        TFColor colorAux = new TFColor();
        
        VoxelGradient voxel_gradient = new VoxelGradient();      
        
        //first we copy most of the max implementation for getting increment vector and nrsamples
                
        //compute the increment and the number of samples
        double[] increments = new double[3];
        VectorMath.setVector(increments, rayVector[0] * sampleStep, rayVector[1] * sampleStep, rayVector[2] * sampleStep);

        // Compute the number of times we need to sample
        //double distance = VectorMath.distance(entryPoint, exitPoint); // is not used
        int nrSamples = 1 + (int) Math.floor(VectorMath.distance(entryPoint, exitPoint) / sampleStep);

        //the current position is initialized as the furtherst point and we work back to the current point
                
        double[] currentPos = new double[3];
        VectorMath.setVector(currentPos, exitPoint[0], exitPoint[1], exitPoint[2]);
                
        //set all colors to zero so background will be black when no data points are passed or only a few.
        voxel_color.r = 0.0;
        voxel_color.g = 0.0;
        voxel_color.b = 0.0;
        voxel_color.b = 1.0;
        
        colorAux.r = 0.0;
        colorAux.g = 0.0;
        colorAux.b = 0.0;
        colorAux.a = 0.0;
        
        //zero gradient
        
        VoxelGradient zero = new VoxelGradient();
        
        //select input funtions from either front or back menu.
            
        TransferFunction tFunc;
        TransferFunction2D tFunc2D;
        RaycastMode mode;
        if (isFront) {
            tFunc = tFuncFront;
            tFunc2D = tFunc2DFront;
            mode = modeFront;
        } else {
            tFunc = tFuncBack;
            tFunc2D = tFunc2DBack;
            mode = modeBack;
        }
                
                
        do {
            short foundValue = getVoxelTrilinear(currentPos);
            VoxelGradient foundGradient = zero;
            
            //check if voxelgradient is even needed
            if(shadingMode || mode == RaycastMode.TRANSFER2D)
            {
                foundGradient = getGradientTrilinear(currentPos);
            }
            TFColor foundColor;
            
            // TODO 2: To be Implemented this function. Now, it just gives back a constant color depending on the mode
            
            
            //compute selected mode.
            
            switch (mode) {
            case COMPOSITING:
                // 1D transfer function 
                foundColor = tFunc.getColor(foundValue);
                opacity = foundColor.a;
                break;
                
                
            case TRANSFER2D:
                // 2D transfer function 
                
                //compute maginitude because this is not done automatically for each gradient object.
                foundColor = tFunc2D.color;
                foundGradient.computeMag();
                opacity = foundColor.a*computeOpacity2DTF(tFunc2D.baseIntensity,tFunc2D.radius,foundValue,foundGradient.mag);

                
                break;
                
            default:
                
                throw new IllegalArgumentException("ModeFront not recognized");
            }
                    
            double tauD = foundColor.a;
            float tauF = (float) foundColor.a;
            //doing the actual composition work per color
            voxel_color.r = tauD * foundColor.r + (1-tauD)*voxel_color.r;
            voxel_color.g = tauD * foundColor.g + (1-tauD)*voxel_color.g;
            voxel_color.b = tauD * foundColor.b + (1-tauD)*voxel_color.b;
            
            //adding multiple opacities described as in floyd paper.
            voxel_color.a = voxel_color.a*(1-opacity);
            
            
            //gradient only relevant for shading so check if we need to compute it
            if(shadingMode)
            {
                voxel_gradient.x = tauF * foundGradient.x + (1-tauF)*voxel_gradient.x;
                voxel_gradient.y = tauF * foundGradient.y + (1-tauF)*voxel_gradient.y;
                voxel_gradient.z = tauF * foundGradient.z + (1-tauF)*voxel_gradient.z;
            }
                    
            //set step towards the entry point
            for (int i = 0; i < 3; i++) {
                currentPos[i] -= increments[i];
            }
            nrSamples--;
        } while (nrSamples > 0);
             

        if (shadingMode) {
            // Apply the shading, again magnitude has to be computed before its use.
            voxel_gradient.computeMag();
            voxel_color = computePhongShading(voxel_color,voxel_gradient,lightVector,rayVector);
        }

        double r = voxel_color.r;
        double g = voxel_color.g;
        double b = voxel_color.b;
        double alpha = (1-voxel_color.a);

        //computes the color
        int color = computePackedPixelColor(r, g, b, alpha);
        return color;
    }

    /**
     * Compute Phong Shading given the voxel color (material color), gradient,
     * light vector and view vector.
     *
     * @param voxel_color Voxel color (material color).
     * @param gradient Gradient voxel.
     * @param lightVector Light vector.
     * @param rayVector View vector.
     * @return Computed color for Phong Shading.
     */
    private TFColor computePhongShading(TFColor voxel_color, VoxelGradient gradient, double[] lightVector,
            double[] rayVector) {
        
        //this function simply implements the matrix formula for the phong shading
        //the same letters are used to indicate the different parts.
        
        double kA = 0.1;
        double kD = 0.7;
        double kS = 0.2;
        
        int alpha = 100;
        
        float nX = gradient.x/gradient.mag;
        float nY = gradient.y/gradient.mag;
        float nZ = gradient.z/gradient.mag;
        
        double dn = (lightVector[0]*nX) + (lightVector[1]*nY) + (lightVector[2]*nZ); //dotproduct n . light
        
        double iDiff = kD * dn;
        
        double rX = lightVector[0] - 2*dn*nX;//mirror light around the n vector
        double rY = lightVector[1] - 2*dn*nY;//r=d???2(d???n)n
        double rZ = lightVector[2] - 2*dn*nZ;
        
        
        double iSpec = kS * Math.pow(((rayVector[0] * rX) +(rayVector[1] * rY) + (rayVector[2] * rZ) ),alpha);
        
        if(iSpec<0)//prevent negative values
        {
            iSpec =0;
        }
        
        if(iDiff<0)
        {
            iDiff = 0;
        }
        //color of ambient light is black 0
        //color of spectral light is 1
                
        double red = 0 * kA + voxel_color.r * iDiff + 1 * iSpec;
        double green = 0 * kA + voxel_color.g * iDiff + 1 * iSpec;
        double blue = 0 * kA + voxel_color.b * iDiff + 1 * iSpec;
       
        
        
        // TODO 7: Implement Phong Shading.
        TFColor color = new TFColor(red, green, blue, voxel_color.a );

        return color;
    }

    /**
     * Implements the basic tracing of rays through the image given the camera
     * transformation. It calls the functions depending on the raycasting mode.
     *
     * @param viewMatrix
     */
    void raycast(double[] viewMatrix) {
        //data allocation
        double[] viewVec = new double[3];
        double[] uVec = new double[3];
        double[] vVec = new double[3];
        double[] pixelCoord = new double[3];
        double[] entryPoint = new double[3];
        double[] exitPoint = new double[3];

        // TODO 5: Limited modification is needed
        // increment in the pixel domain in pixel units
        
        int increment = 1;
        // sample step in voxel units
        int sampleStep = 1;
        
        //if we are interacting with the model we use more coarse settings
        if(interactiveMode)
        {
            increment = 3;
            
            sampleStep = 3;
        }
        
        // reset the image to black
        resetImage();

        // vector uVec and vVec define a plane through the origin,
        // perpendicular to the view vector viewVec which is going from the view point towards the object
        // uVec contains the up vector of the camera in world coordinates (image vertical)
        // vVec contains the horizontal vector in world coordinates (image horizontal)
        VectorMath.setVector(viewVec, viewMatrix[2], viewMatrix[6], viewMatrix[10]);
        VectorMath.setVector(uVec, viewMatrix[0], viewMatrix[4], viewMatrix[8]);
        VectorMath.setVector(vVec, viewMatrix[1], viewMatrix[5], viewMatrix[9]);

        // We get the size of the image/texture we will be puting the result of the 
        // volume rendering operation.
        int imageW = image.getWidth();
        int imageH = image.getHeight();

        int[] imageCenter = new int[2];
        // Center of the image/texture 
        imageCenter[0] = imageW / 2;
        imageCenter[1] = imageH / 2;

        //The rayVector is pointing towards the scene
        double[] rayVector = new double[3];
        rayVector[0] = -viewVec[0];
        rayVector[1] = -viewVec[1];
        rayVector[2] = -viewVec[2];

        // compute the volume center
        double[] volumeCenter = new double[3];
        VectorMath.setVector(volumeCenter, volume.getDimX() / 2, volume.getDimY() / 2, volume.getDimZ() / 2);

        // ray computation for each pixel
        for (int j = imageCenter[1] - imageH / 2; j < imageCenter[1] + imageH / 2; j += increment) {
            for (int i = imageCenter[0] - imageW / 2; i < imageCenter[0] + imageW / 2; i += increment) {
                // compute starting points of rays in a plane shifted backwards to a position behind the data set
                computePixelCoordinatesBehindFloat(pixelCoord, viewVec, uVec, vVec, i, j);
                // compute the entry and exit point of the ray
                computeEntryAndExit(pixelCoord, rayVector, entryPoint, exitPoint);

                // TODO 9: Implement logic for cutting plane.
                RaycastMode raycastMode = modeFront;
                boolean isFront = true;
                
                if(cuttingPlaneMode)
                {
                    double[] emptyVec = new double[3];
                    //computing dotproduct
                    double decider = VectorMath.dotproduct(planeNorm, VectorMath.difference(planePoint, entryPoint, emptyVec));
                    
                    //if decider is smaller then 0 it means the pixel is behind the plane
                    if (decider <= 0) {
                        raycastMode = modeBack;
                        isFront = false;
                    } 
                }
                
                
                  
                if ((entryPoint[0] > -1.0) && (exitPoint[0] > -1.0)) {
                    int val = 0;
                    switch (raycastMode) {
                        case COMPOSITING:
                        case TRANSFER2D:
                            val = traceRayComposite(entryPoint, exitPoint, rayVector, sampleStep, isFront);
                            break;
                        case MIP:
                            val = traceRayMIP(entryPoint, exitPoint, rayVector, sampleStep);
                            break;
                        case ISO_SURFACE:
                            val = traceRayIso(entryPoint, exitPoint, rayVector, sampleStep,isFront);
                            break;
                    }
                    //when going low resolution there is the possiblily you draw outside the box, this is prevented.
                    //The pixels are not drawn, in normal mode this does not occur
                    if(i+increment<imageW && j+increment<imageH)
                    {
                        for (int ii = i; ii < i + increment; ii++) {
                            for (int jj = j; jj < j + increment; jj++) {
                                image.setRGB(ii, jj, val);
                            }
                        }
                    }
                }

            }
        }
    }

    /**
     * Computes the opacity based on the value of the pixel and values of the
     * triangle widget. {@link #tFunc2DFront} contains the values of the base
     * intensity and radius. {@link TransferFunction2D#baseIntensity} and
     * {@link TransferFunction2D#radius} are in image intensity units.
     *
     * @param material_value Value of the material.
     * @param material_r Radius of the material.
     * @param voxelValue Voxel value.
     * @param gradMagnitude Gradient magnitude.
     * @return
     */
    public double computeOpacity2DTF(double material_value, double material_r,
            double voxelValue, double gradMagnitude) {
        
        // TODO 8: Implement weight based opacity.
        
        //nomalisation of the radius, as shown by teacher in an announcement
        double radius = material_r / gradients.getMaxGradientMagnitude();
        
        //levoys approach of determining the opacity
        if (gradMagnitude == 0.0 && voxelValue == material_value)
        {
            return 1.0;
        }
        else
        {
            if(gradMagnitude>0 && voxelValue - (radius*gradMagnitude) <= material_value && material_value <= voxelValue + (radius*gradMagnitude))
            {
                return 1.0-((1.0/radius)*Math.abs((material_value-voxelValue)/(gradMagnitude)));
            }
            else
            {
                return 0.0;
            }
        }
            
        
        
    }

    /**
     * Class constructor. Initializes attributes.
     */
    public RaycastRenderer() {
        panelFront = new RaycastRendererPanel(this);
        panelFront.setSpeedLabel("0");

        isoColorFront = new TFColor();
        isoColorFront.r = 1.0;
        isoColorFront.g = 1.0;
        isoColorFront.b = 0.0;
        isoColorFront.a = 1.0;

        isoColorBack = new TFColor();
        isoColorBack.r = 1.0;
        isoColorBack.g = 1.0;
        isoColorBack.b = 0.0;
        isoColorBack.a = 1.0;

        modeFront = RaycastMode.SLICER;
        modeBack = RaycastMode.SLICER;
    }

    /**
     * Sets the volume to be visualized. It creates the Image buffer for the
     * size of the volume. Initializes the transfers functions
     *
     * @param vol Volume to be visualized.
     */
    public void setVolume(Volume vol) {
        System.out.println("Assigning volume");
        volume = vol;

        System.out.println("Computing gradients");
        gradients = new GradientVolume(vol);

        // set up image for storing the resulting rendering
        // the image width and height are equal to the length of the volume diagonal
        int imageSize = (int) Math.floor(Math.sqrt(vol.getDimX() * vol.getDimX() + vol.getDimY() * vol.getDimY()
                + vol.getDimZ() * vol.getDimZ()));
        if (imageSize % 2 != 0) {
            imageSize = imageSize + 1;
        }

        image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);

        // Initialize transfer function and GUI panels
        tFuncFront = new TransferFunction(volume.getMinimum(), volume.getMaximum());
        tFuncFront.setTestFunc();
        tFuncFront.addTFChangeListener(this);
        tfEditor = new TransferFunctionEditor(tFuncFront, volume.getHistogram());

        tFunc2DFront = new TransferFunction2D((short) (volume.getMaximum() / 2), 0.2 * volume.getMaximum());
        tfEditor2DFront = new TransferFunction2DEditor(tFunc2DFront, volume, gradients);
        tfEditor2DFront.addTFChangeListener(this);

        // Initialize transfer function and GUI panels for cutting plane
        tFuncBack = new TransferFunction(volume.getMinimum(), volume.getMaximum());
        tFuncBack.setTestFunc();
        tFuncBack.addTFChangeListener(this);
        tfEditorBack = new TransferFunctionEditor(tFuncBack, volume.getHistogram());

        tFunc2DBack = new TransferFunction2D((short) (volume.getMaximum() / 2), 0.2 * volume.getMaximum());
        tfEditor2DBack = new TransferFunction2DEditor(tFunc2DBack, volume, gradients);
        tfEditor2DBack.addTFChangeListener(this);

        // Set plane point
        VectorMath.setVector(planePoint, volume.getDimX() / 2, volume.getDimY() / 2, volume.getDimZ() / 2);

        System.out.println("Finished initialization of RaycastRenderer");
    }

    /**
     * Do NOT modify.
     *
     * Visualizes the volume. It calls the corresponding render functions.
     *
     * @param gl OpenGL API.
     */
    @Override
    public void visualize(GL2 gl) {
        if (volume == null) {
            return;
        }

        drawBoundingBox(gl);

        // If mode is Cutting Plane, draw the cutting plane.
        if (cuttingPlaneMode) {
            drawCuttingPlane(gl);
        }

        gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, _viewMatrix, 0);

        long startTime = System.currentTimeMillis();

        switch (modeFront) {
            case SLICER:
                slicer(_viewMatrix);
                break;
            default:
                // Default case raycast
                raycast(_viewMatrix);
                break;
        }

        long endTime = System.currentTimeMillis();
        double runningTime = (endTime - startTime);
        panelFront.setSpeedLabel(Double.toString(runningTime));

        Texture texture = AWTTextureIO.newTexture(gl.getGLProfile(), image, false);

        gl.glPushAttrib(GL2.GL_LIGHTING_BIT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        // draw rendered image as a billboard texture
        texture.enable(gl);
        texture.bind(gl);
        double halfWidth = image.getWidth() / 2.0;
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glBegin(GL2.GL_QUADS);
        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glTexCoord2d(0.0, 0.0);
        gl.glVertex3d(-halfWidth, -halfWidth, 0.0);
        gl.glTexCoord2d(0.0, 1.0);
        gl.glVertex3d(-halfWidth, halfWidth, 0.0);
        gl.glTexCoord2d(1.0, 1.0);
        gl.glVertex3d(halfWidth, halfWidth, 0.0);
        gl.glTexCoord2d(1.0, 0.0);
        gl.glVertex3d(halfWidth, -halfWidth, 0.0);
        gl.glEnd();
        texture.disable(gl);
        texture.destroy(gl);
        gl.glPopMatrix();

        gl.glPopAttrib();

        if (gl.glGetError() > 0) {
            System.out.println("some OpenGL error: " + gl.glGetError());
        }
    }

    public RaycastMode getRaycastMode() {
        return modeFront;
    }

    /**
     * Sets the raycast mode to the specified one.
     *
     * @param mode New Raycast mode.
     */
    public void setRaycastModeFront(RaycastMode mode) {
        this.modeFront = mode;
    }

    public void setRaycastModeBack(RaycastMode mode) {
        this.modeBack = mode;
    }

    @Override
    public void changed() {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).changed();
        }
    }

    /**
     * Do NOT modify.
     *
     * Updates the vectors that represent the cutting plane.
     *
     * @param d View Matrix.
     */
    public void updateCuttingPlaneVectors(double[] d) {
        VectorMath.setVector(_planeU, d[1], d[5], d[9]);
        VectorMath.setVector(_planeV, d[2], d[6], d[10]);
        VectorMath.setVector(planeNorm, d[0], d[4], d[8]);
    }

    /**
     * Sets the cutting plane mode flag.
     *
     * @param cuttingPlaneMode
     */
    public void setCuttingPlaneMode(boolean cuttingPlaneMode) {
        this.cuttingPlaneMode = cuttingPlaneMode;
    }

    public boolean isCuttingPlaneMode() {
        return cuttingPlaneMode;
    }

    /**
     * Sets shading mode flag.
     *
     * @param shadingMode
     */
    public void setShadingMode(boolean shadingMode) {
        this.shadingMode = shadingMode;
    }

    public RaycastRendererPanel getPanel() {
        return panelFront;
    }

    public TransferFunction2DEditor getTF2DPanel() {
        return tfEditor2DFront;
    }

    public TransferFunctionEditor getTFPanel() {
        return tfEditor;
    }

    public TransferFunction2DEditor getTF2DPanelBack() {
        return tfEditor2DBack;
    }

    public TransferFunctionEditor getTFPanelBack() {
        return tfEditorBack;
    }

    //////////////////////////////////////////////////////////////////////
    /////////////////// PRIVATE FUNCTIONS AND ATTRIBUTES /////////////////
    //////////////////////////////////////////////////////////////////////
    /**
     * OpenGL View Matrix. The shape (4x4) remains constant.
     */
    private final double[] _viewMatrix = new double[4 * 4];

    /**
     * Vector used to draw the cutting plane.
     */
    private final double[] _planeU = new double[3];

    /**
     * Vector used to draw the cutting plane.
     */
    private final double[] _planeV = new double[3];

    /**
     * Do NOT modify.
     *
     * Draws the bounding box around the volume.
     *
     * @param gl OpenGL API.
     */
    private void drawBoundingBox(GL2 gl) {
        gl.glPushAttrib(GL2.GL_CURRENT_BIT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glColor4d(1.0, 1.0, 1.0, 1.0);
        gl.glLineWidth(1.5f);
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glDisable(GL.GL_LINE_SMOOTH);
        gl.glDisable(GL.GL_BLEND);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glPopAttrib();
    }

    /**
     * Do NOT modify.
     *
     * Draws the cutting plane through.
     *
     * @param gl OpenGL API.
     */
    private void drawCuttingPlane(GL2 gl) {
        gl.glPushAttrib(GL2.GL_CURRENT_BIT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glColor4d(1.0, 1.0, 1.0, 1.0);
        gl.glLineWidth(2f);
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        double D = Math.sqrt(Math.pow(volume.getDimX(), 2) + Math.pow(volume.getDimY(), 2) + Math.pow(volume.getDimZ(), 2)) / 2;

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-_planeU[0] * D - _planeV[0] * D, -_planeU[1] * D - _planeV[1] * D, -_planeU[2] * D - _planeV[2] * D);
        gl.glVertex3d(_planeU[0] * D - _planeV[0] * D, _planeU[1] * D - _planeV[1] * D, _planeU[2] * D - _planeV[2] * D);
        gl.glVertex3d(_planeU[0] * D + _planeV[0] * D, _planeU[1] * D + _planeV[1] * D, _planeU[2] * D + _planeV[2] * D);
        gl.glVertex3d(-_planeU[0] * D + _planeV[0] * D, -_planeU[1] * D + _planeV[1] * D, -_planeU[2] * D + _planeV[2] * D);
        gl.glEnd();

        gl.glDisable(GL.GL_LINE_SMOOTH);
        gl.glDisable(GL.GL_BLEND);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glPopAttrib();
    }

    /**
     * Do NOT modify this function.
     *
     * Sets the Iso value.
     *
     * @param newColor
     */
    public void setIsoValueFront(float isoValueFront) {
        this.isoValueFront = isoValueFront;
    }

    /**
     * Do NOT modify this function.
     *
     * Sets the Iso value.
     *
     * @param newColor
     */
    public void setIsoValueBack(float isoValueBack) {
        this.isoValueBack = isoValueBack;
    }

    /**
     * Do NOT modify this function.
     *
     * Sets the Iso Color.
     *
     * @param newColor
     */
    public void setIsoColorFront(TFColor newColor) {
        this.isoColorFront.r = newColor.r;
        this.isoColorFront.g = newColor.g;
        this.isoColorFront.b = newColor.b;
    }

    /**
     * Do NOT modify this function.
     *
     * Sets the Iso Color.
     *
     * @param newColor
     */
    public void setIsoColorBack(TFColor newColor) {
        this.isoColorBack.r = newColor.r;
        this.isoColorBack.g = newColor.g;
        this.isoColorBack.b = newColor.b;
    }

    /**
     * Do NOT modify this function.
     *
     * Resets the image with 0 values.
     */
    private void resetImage() {
        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                image.setRGB(i, j, 0);
            }
        }
    }

    /**
     * Do NOT modify this function.
     *
     * Computes the increments according to sample step and stores the result in
     * increments.
     *
     * @param increments Vector to store the result.
     * @param rayVector Ray vector.
     * @param sampleStep Sample step.
     */
    private void computeIncrementsB2F(double[] increments, double[] rayVector, double sampleStep) {
        // we compute a back to front compositing so we start increments in the oposite direction than the pixel ray
        VectorMath.setVector(increments, -rayVector[0] * sampleStep, -rayVector[1] * sampleStep, -rayVector[2] * sampleStep);
    }

    /**
     * Do NOT modify this function.
     *
     * Packs a color into a Integer.
     *
     * @param r Red component of the color.
     * @param g Green component of the color.
     * @param b Blue component of the color.
     * @param a Alpha component of the color.
     * @return
     */
    private static int computePackedPixelColor(double r, double g, double b, double a) {
        int c_alpha = a <= 1.0 ? (int) Math.floor(a * 255) : 255;
        int c_red = r <= 1.0 ? (int) Math.floor(r * 255) : 255;
        int c_green = g <= 1.0 ? (int) Math.floor(g * 255) : 255;
        int c_blue = b <= 1.0 ? (int) Math.floor(b * 255) : 255;
        int pixelColor = (c_alpha << 24) | (c_red << 16) | (c_green << 8) | c_blue;
        return pixelColor;
    }

    /**
     * Do NOT modify this function.
     *
     * Computes the entry and exit of a view vector with respect the faces of
     * the volume.
     *
     * @param p Point of the ray.
     * @param viewVec Direction of the ray.
     * @param entryPoint Vector to store entry point.
     * @param exitPoint Vector to store exit point.
     */
    private void computeEntryAndExit(double[] p, double[] viewVec, double[] entryPoint, double[] exitPoint) {

        for (int i = 0; i < 3; i++) {
            entryPoint[i] = -1;
            exitPoint[i] = -1;
        }

        double[] plane_pos = new double[3];
        double[] plane_normal = new double[3];
        double[] intersection = new double[3];

        VectorMath.setVector(plane_pos, volume.getDimX(), 0, 0);
        VectorMath.setVector(plane_normal, 1, 0, 0);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);

        VectorMath.setVector(plane_pos, 0, 0, 0);
        VectorMath.setVector(plane_normal, -1, 0, 0);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);

        VectorMath.setVector(plane_pos, 0, volume.getDimY(), 0);
        VectorMath.setVector(plane_normal, 0, 1, 0);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);

        VectorMath.setVector(plane_pos, 0, 0, 0);
        VectorMath.setVector(plane_normal, 0, -1, 0);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);

        VectorMath.setVector(plane_pos, 0, 0, volume.getDimZ());
        VectorMath.setVector(plane_normal, 0, 0, 1);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);

        VectorMath.setVector(plane_pos, 0, 0, 0);
        VectorMath.setVector(plane_normal, 0, 0, -1);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);
    }

    /**
     * Do NOT modify this function.
     *
     * Checks if a line intersects a plane.
     *
     * @param plane_pos Position of plane.
     * @param plane_normal Normal of plane.
     * @param line_pos Position of line.
     * @param line_dir Direction of line.
     * @param intersection Vector to store intersection.
     * @return True if intersection happens. False otherwise.
     */
    private static boolean intersectLinePlane(double[] plane_pos, double[] plane_normal,
            double[] line_pos, double[] line_dir, double[] intersection) {

        double[] tmp = new double[3];

        for (int i = 0; i < 3; i++) {
            tmp[i] = plane_pos[i] - line_pos[i];
        }

        double denom = VectorMath.dotproduct(line_dir, plane_normal);
        if (Math.abs(denom) < 1.0e-8) {
            return false;
        }

        double t = VectorMath.dotproduct(tmp, plane_normal) / denom;

        for (int i = 0; i < 3; i++) {
            intersection[i] = line_pos[i] + t * line_dir[i];
        }

        return true;
    }

    /**
     * Do NOT modify this function.
     *
     * Checks if it is a valid intersection.
     *
     * @param intersection Vector with the intersection point.
     * @param xb
     * @param xe
     * @param yb
     * @param ye
     * @param zb
     * @param ze
     * @return
     */
    private static boolean validIntersection(double[] intersection, double xb, double xe, double yb,
            double ye, double zb, double ze) {

        return (((xb - 0.5) <= intersection[0]) && (intersection[0] <= (xe + 0.5))
                && ((yb - 0.5) <= intersection[1]) && (intersection[1] <= (ye + 0.5))
                && ((zb - 0.5) <= intersection[2]) && (intersection[2] <= (ze + 0.5)));

    }

    /**
     * Do NOT modify this function.
     *
     * Checks the intersection of a line with a plane and returns entry and exit
     * points in case intersection happens.
     *
     * @param plane_pos Position of plane.
     * @param plane_normal Normal vector of plane.
     * @param line_pos Position of line.
     * @param line_dir Direction of line.
     * @param intersection Vector to store the intersection point.
     * @param entryPoint Vector to store the entry point.
     * @param exitPoint Vector to store the exit point.
     */
    private void intersectFace(double[] plane_pos, double[] plane_normal,
            double[] line_pos, double[] line_dir, double[] intersection,
            double[] entryPoint, double[] exitPoint) {

        boolean intersect = intersectLinePlane(plane_pos, plane_normal, line_pos, line_dir,
                intersection);
        if (intersect) {

            double xpos0 = 0;
            double xpos1 = volume.getDimX();
            double ypos0 = 0;
            double ypos1 = volume.getDimY();
            double zpos0 = 0;
            double zpos1 = volume.getDimZ();

            if (validIntersection(intersection, xpos0, xpos1, ypos0, ypos1,
                    zpos0, zpos1)) {
                if (VectorMath.dotproduct(line_dir, plane_normal) < 0) {
                    entryPoint[0] = intersection[0];
                    entryPoint[1] = intersection[1];
                    entryPoint[2] = intersection[2];
                } else {
                    exitPoint[0] = intersection[0];
                    exitPoint[1] = intersection[1];
                    exitPoint[2] = intersection[2];
                }
            }
        }
    }

    /**
     * Do NOT modify this function.
     *
     * Calculates the pixel coordinate for the given parameters.
     *
     * @param pixelCoord Vector to store the result.
     * @param volumeCenter Location of the center of the volume.
     * @param uVec uVector.
     * @param vVec vVector.
     * @param i Pixel i.
     * @param j Pixel j.
     */
    private void computePixelCoordinatesFloat(double pixelCoord[], double volumeCenter[], double uVec[], double vVec[], float i, float j) {
        // Coordinates of a plane centered at the center of the volume (volumeCenter and oriented according to the plane defined by uVec and vVec
        float imageCenter = image.getWidth() / 2;
        pixelCoord[0] = uVec[0] * (i - imageCenter) + vVec[0] * (j - imageCenter) + volumeCenter[0];
        pixelCoord[1] = uVec[1] * (i - imageCenter) + vVec[1] * (j - imageCenter) + volumeCenter[1];
        pixelCoord[2] = uVec[2] * (i - imageCenter) + vVec[2] * (j - imageCenter) + volumeCenter[2];
    }

    /**
     * Do NOT modify this function.
     *
     * Same as
     * {@link RaycastRenderer#computePixelCoordinatesFloat(double[], double[], double[], double[], float, float)}
     * but for integer pixel coordinates.
     *
     * @param pixelCoord Vector to store the result.
     * @param volumeCenter Location of the center of the volume.
     * @param uVec uVector.
     * @param vVec vVector.
     * @param i Pixel i.
     * @param j Pixel j.
     */
    private void computePixelCoordinates(double pixelCoord[], double volumeCenter[], double uVec[], double vVec[], int i, int j) {
        // Coordinates of a plane centered at the center of the volume (volumeCenter and oriented according to the plane defined by uVec and vVec
        int imageCenter = image.getWidth() / 2;
        pixelCoord[0] = uVec[0] * (i - imageCenter) + vVec[0] * (j - imageCenter) + volumeCenter[0];
        pixelCoord[1] = uVec[1] * (i - imageCenter) + vVec[1] * (j - imageCenter) + volumeCenter[1];
        pixelCoord[2] = uVec[2] * (i - imageCenter) + vVec[2] * (j - imageCenter) + volumeCenter[2];
    }

    /**
     * Do NOT modify this function.
     *
     * Calculates the pixel coordinate for the given parameters. It calculates
     * the coordinate having the center (0,0) of the view plane aligned with the
     * center of the volume and moved a distance equivalent to the diagonal to
     * make sure we are far enough.
     *
     * @param pixelCoord Vector to store the result.
     * @param viewVec View vector (ray).
     * @param uVec uVector.
     * @param vVec vVector.
     * @param i Pixel i.
     * @param j Pixel j.
     */
    private void computePixelCoordinatesBehindFloat(double pixelCoord[], double viewVec[], double uVec[], double vVec[], float i, float j) {
        int imageCenter = image.getWidth() / 2;
        // Pixel coordinate is calculate having the center (0,0) of the view plane aligned with the center of the volume and moved a distance equivalent
        // to the diaganal to make sure I am far away enough.

        double diagonal = Math.sqrt((volume.getDimX() * volume.getDimX()) + (volume.getDimY() * volume.getDimY()) + (volume.getDimZ() * volume.getDimZ())) / 2;
        pixelCoord[0] = uVec[0] * (i - imageCenter) + vVec[0] * (j - imageCenter) + viewVec[0] * diagonal + volume.getDimX() / 2.0;
        pixelCoord[1] = uVec[1] * (i - imageCenter) + vVec[1] * (j - imageCenter) + viewVec[1] * diagonal + volume.getDimY() / 2.0;
        pixelCoord[2] = uVec[2] * (i - imageCenter) + vVec[2] * (j - imageCenter) + viewVec[2] * diagonal + volume.getDimZ() / 2.0;
    }

    /**
     * Do NOT modify this function.
     *
     * Same as
     * {@link RaycastRenderer#computePixelCoordinatesBehindFloat(double[], double[], double[], double[], int, int)}
     * but for integer pixel coordinates.
     *
     * @param pixelCoord Vector to store the result.
     * @param viewVec View vector (ray).
     * @param uVec uVector.
     * @param vVec vVector.
     * @param i Pixel i.
     * @param j Pixel j.
     */
    private void computePixelCoordinatesBehind(double pixelCoord[], double viewVec[], double uVec[], double vVec[], int i, int j) {
        int imageCenter = image.getWidth() / 2;
        // Pixel coordinate is calculate having the center (0,0) of the view plane aligned with the center of the volume and moved a distance equivalent
        // to the diaganal to make sure I am far away enough.

        double diagonal = Math.sqrt((volume.getDimX() * volume.getDimX()) + (volume.getDimY() * volume.getDimY()) + (volume.getDimZ() * volume.getDimZ())) / 2;
        pixelCoord[0] = uVec[0] * (i - imageCenter) + vVec[0] * (j - imageCenter) + viewVec[0] * diagonal + volume.getDimX() / 2.0;
        pixelCoord[1] = uVec[1] * (i - imageCenter) + vVec[1] * (j - imageCenter) + viewVec[1] * diagonal + volume.getDimY() / 2.0;
        pixelCoord[2] = uVec[2] * (i - imageCenter) + vVec[2] * (j - imageCenter) + viewVec[2] * diagonal + volume.getDimZ() / 2.0;
    }
}

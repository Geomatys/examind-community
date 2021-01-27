/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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
package org.constellation.portrayal;

import java.awt.*;
import java.awt.image.BufferedImage;
import org.geotoolkit.display.PortrayalException;
import org.geotoolkit.display.canvas.control.CanvasMonitor;
import org.geotoolkit.display.canvas.control.NeverFailMonitor;
import org.geotoolkit.display.canvas.control.StopOnErrorMonitor;
import org.geotoolkit.display2d.service.CanvasDef;
import org.geotoolkit.display2d.service.DefaultPortrayalService;
import org.geotoolkit.display2d.service.OutputDef;
import org.geotoolkit.display2d.service.SceneDef;
import org.geotoolkit.display2d.service.VisitDef;


/**
 * TODO: check if this overlay is really needed. It does weird stuff like clearing MapLayers object after use.
 * Points to check:
 *  - Is MapLayers clearing really needed ?
 *  - Does it contain any benefit / overriden behavior to default Geotk portrayal service ?
 *
 * Service class to portray or work with two dimensional scenes defined by a
 * scene definition, a view definition, and a canvas definition.
 *
 * @author Johann Sorel (Geomatys)
 * @author Cédric Briançon (Geomatys)
 */
public final class CstlPortrayalService {

    private static final CstlPortrayalService INSTANCE = new CstlPortrayalService();

    /**
     * @return a singleton of cstlPortralyalService
     */
    public static CstlPortrayalService getInstance(){
        return INSTANCE;
    }

    private CstlPortrayalService(){}

    /**
     * Portray a set of Layers over a given geographic extent with a given
     * resolution yielding a {@code BufferedImage} of the scene.
     * @param sdef A structure which defines the scene.
     * @param cdef A structure which defines the canvas.
     *
     * @return A rendered image of the scene, in the chosen view and for the
     *           given canvas.
     * @throws PortrayalException For errors during portrayal, TODO: common examples?
     */
    public BufferedImage portray( final SceneDef sdef,
                                  final CanvasDef cdef)
    		throws PortrayalException {

        final StopOnErrorMonitor monitor = new StopOnErrorMonitor();
        cdef.setMonitor(monitor);

        try {
            final BufferedImage buffer = DefaultPortrayalService.portray(cdef,sdef);

            final Exception exp = monitor.getLastException();
            if(exp != null){
                throw exp;
            }

            return buffer;

        } catch(Exception ex) {
            if (ex instanceof PortrayalException) {
                throw (PortrayalException)ex;
            } else {
                throw new PortrayalException(ex);
            }
        } finally {
            sdef.getContext().getComponents().clear();
        }

    }

    /**
     * Apply the Visitor to all the
     * {@link org.opengis.display.primitive.Graphic} objects which lie within
     * the {@link java.awt.Shape} in the given scene.
     * <p>
     * The visitor could be an extension of the AbstractGraphicVisitor class in
     * this same package.
     * </p>
     *
     * TODO: why are the last two arguments not final?
     *
     */
    public void visit( final SceneDef sdef,
                       final CanvasDef cdef,
                       final VisitDef visitDef)
            throws PortrayalException {

        try{
            DefaultPortrayalService.visit(cdef,sdef,visitDef);
        }catch(Exception ex){
            if (ex instanceof PortrayalException) {
                throw (PortrayalException)ex;
            } else {
                throw new PortrayalException(ex);
            }
        }finally{
            visitDef.getVisitor().endVisit();
            sdef.getContext().getComponents().clear();
        }

    }

    /**
     * Creates an image of the given {@link Exception}. This is useful for
     * several OGC web services which need to record that an exception has
     * occurred but only return an image in the message exchange protocol.
     *
     * TODO: document how the size of the text should be chosen.
     *
     * @param e      The exception to document in the generated image.
     * @param dim    The dimension in pixels of the generated image.
     * @return       An image of the exception message text. TODO: verify this.
     */
    public BufferedImage writeInImage(Exception e, Dimension dim){
        return DefaultPortrayalService.writeException(e, dim, false, Color.BLACK);
    }

    /**
     *  Creates a blank image fill with given color. This is useful for
     *  several OGC web services which need to return blank image/tile
     *  when an exception occurred
     *
     * @param color The color of output image.
     * @param dim   The dimension in pixels of the generated image.
     * @return      An image with all pixel at the same color.
     */
    public BufferedImage writeBlankImage(Color color, Dimension dim) {
        final BufferedImage img = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, dim.width, dim.height);
        g.dispose();
        return img;
    }

    /**
     * Portray a set of Layers over a given geographic extent with a given
     * resolution in the provided output.
     * @param sdef A structure which defines the scene.
     * @param cdef A structure which defines the canvas.
     * @param odef A structure which defines the output.
     *
     * @throws PortrayalException For errors during portrayal, TODO: common examples?
     */
    public void portray(SceneDef sdef, CanvasDef cdef, OutputDef odef) throws PortrayalException {

        //never stop rendering, we write in the output, we must never.
        final CanvasMonitor monitor = new NeverFailMonitor();
        cdef.setMonitor(monitor);

        try {
            DefaultPortrayalService.portray(cdef,sdef,odef);
        }catch(PortrayalException ex){
            throw ex;
        } catch(Exception ex) {
            throw new PortrayalException(ex);
        } finally {
            sdef.getContext().getComponents().clear();
        }
    }

}

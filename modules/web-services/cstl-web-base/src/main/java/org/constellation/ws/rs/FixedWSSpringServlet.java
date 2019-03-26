
package org.constellation.ws.rs;

import com.sun.xml.ws.transport.http.servlet.WSSpringServlet;

/**
 *
 * @author guilhem
 */
public class FixedWSSpringServlet extends WSSpringServlet {

    @Override
    public void destroy() {
        // do nothing this is the fix.
    }
    
}

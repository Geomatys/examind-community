package com.examind.wps.component;

import com.examind.wps.api.ExecutionStatus;
import java.util.List;
import java.util.stream.Stream;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.wps.xml.v200.Execute;
import org.geotoolkit.wps.xml.v200.ProcessDescription;
import org.geotoolkit.wps.xml.v200.ProcessOffering;
import com.examind.wps.api.WebProcessingComponent;
import com.examind.wps.util.WPSUtils;
import java.util.Locale;
import org.geotoolkit.wps.xml.v200.ProcessProperties;

/**
 * TODO: Try to use Quartz (see WPSScheduler class in cstl-web-wps module). For
 * now we stick to already used method which consists of using a standard Java
 * executor.
 *
 * @author Alexis Manin (Geomatys)
 */
public class GeotkQuartzComponent implements WebProcessingComponent {

    final List<ProcessDescriptor> processes;
    final WPSScheduler scheduler;

    public GeotkQuartzComponent(final List<ProcessDescriptor> processes, final WPSScheduler scheduler) {
        this.processes = processes;
        this.scheduler = scheduler;
    }

    @Override
    public Stream<ProcessProperties> list(Locale lang) {
        // TODO : improve. We should avoid hard coded cast. The used method signature should be updated.
        return processes.stream().map(p -> (ProcessProperties) WPSUtils.generateProcessBrief(p, lang));
    }

    @Override
    public ProcessDescription describe(ProcessProperties process, Locale lang) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ExecutionStatus execute(Execute request) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}

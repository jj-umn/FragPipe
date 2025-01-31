/* 
 * Copyright (C) 2018 Dmitry Avtonomov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.dmtavt.fragpipe.tools.fragger;

import java.nio.file.Path;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.dmtavt.fragpipe.api.VersionFetcher;
import com.dmtavt.fragpipe.params.ThisAppProps;
import com.github.chhh.utils.PropertiesUtils;

/**
 *
 * @author Dmitry Avtonomov
 */
public class MsfraggerVersionFetcherGithub implements VersionFetcher {
    private static final Logger log = LoggerFactory.getLogger(MsfraggerVersionFetcherGithub.class);
    String downloadUrl = "";
    
    @Override
    public String fetchVersion() {
        Properties props = ThisAppProps.getRemoteProperties();
        if (props == null) {
            log.debug("Didn't get msfragger update info from any of the sources");
            props = PropertiesUtils.loadPropertiesLocal(MsfraggerProps.class, MsfraggerProps.PROPERTIES_FILE_NAME);
        }

        final String latestKnownVer = props.getProperty(MsfraggerProps.PROP_LATEST_VERSION);
        if (latestKnownVer == null) {
            log.debug(String.format("Msfragger property '%s' was not found", MsfraggerProps.PROP_LATEST_VERSION));
            return null;
        }
        downloadUrl = props.getProperty(MsfraggerProps.PROP_UPDATESERVER_WEBSITE_URL);
        if (downloadUrl == null)
            throw new IllegalStateException(String.format("Msfragger property '%s' was not found",
                    MsfraggerProps.PROP_UPDATESERVER_WEBSITE_URL));
        
        return latestKnownVer;
    }

    @Override
    public String getToolName() {
        return MsfraggerProps.PROGRAM_NAME;
    }

    @Override
    public String getDownloadUrl() {
        return downloadUrl;
    }

    @Override
    public boolean canAutoUpdate() {
        return false;
    }

    @Override
    public Path autoUpdate(Path p) {
        throw new UnsupportedOperationException("Not supported.");
    }
    
}

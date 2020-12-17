/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.controller;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletContext;

/**
 * @author florian
 *
 */
public class FileResourceManager {
    
    private final Path coreResourcesPath;
    private final Path themResourcesPath;
    private final ServletContext context;
    private final String theme;
    
    /**
     * 
     * @param servletContex The servletContext of the application
     * @param themeRootPath A file system path containing the root path of a theme repository. May be null to use the internal resource path for theme resources
     */
    public FileResourceManager(ServletContext servletContex, String theme) {
        this.coreResourcesPath = Paths.get(servletContex.getRealPath("resources"));
        this.themResourcesPath = Paths.get(servletContex.getRealPath("resources/themes/" + theme));
        this.context = servletContex;
        this.theme = theme;
    }
    
    public Path getCoreResourcePath(String resource) {
        String path = context.getRealPath("resources/" + resource);
        return Paths.get(path);
    }

    public Path getThemeResourcePath(String resource) {
        Path path = this.themResourcesPath.resolve(resource);
        return path;
    }
    
    public URI getCoreResourceURI(String resource) {
        URI uri = URI.create("/resources/" + resource);
        return uri;
    }

    public URI getThemeResourceURI(String resource) {
        URI uri = URI.create("/resources/themes/" + this.theme + "/" + resource);
        return uri;
    }
}

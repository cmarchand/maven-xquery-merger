package com.oxiane.maven.xqueryMerger;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
import com.oxiane.xquery.merger.XQueryMerger;
import com.oxiane.xquery.merger.utils.ParsingException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

/**
 * Reads a XQuery file and merges into it all XQuery modules imported and availables.
 * XQuery modules imported but not availables are considered as available on the target XQuery processor,
 * so the <tt>import module namespace='xxx' at 'xxx.xqm';</tt> is not modified.
 *
 */
@Mojo(name = "merge-xquery", defaultPhase = LifecyclePhase.COMPILE)
public class Merger extends AbstractMojo {

    /**
     * Location of source files
     */
    @Parameter(defaultValue="${project.basedir}/src/main/xquery")
    private File inputSources;
    /**
     * Destination of merge
     */
    @Parameter(defaultValue="${project.basedir}/target/xquery")
    private File outputDirectory;
    /**
     * Filter that accepts files to merge. Default is '*.xq'
     */
    @Parameter(defaultValue="*.xq")
    private String acceptFilter;

    @Override
    public void execute() throws MojoExecutionException {
        Path sourceRootPath = inputSources.toPath();
        Path destinationRootPath = outputDirectory.toPath();
        IOFileFilter filter = buildFilter();
        Iterator<File> it = FileUtils.iterateFiles(inputSources, filter, FileFilterUtils.directoryFileFilter());
        if(!it.hasNext()) {
            getLog().warn("No file found matching "+filter.toString()+" in "+inputSources.getAbsolutePath());
        }
        while(it.hasNext()) {
            File sourceFile = it.next();
            Path fileSourcePath = sourceFile.toPath();
            Path relativePath = sourceRootPath.relativize(fileSourcePath);
            getLog().debug("[Merger] found source: "+fileSourcePath.toString());
            getLog().debug("[Merger]    relative path is "+relativePath.toString());
            StreamSource source = new StreamSource(sourceFile);
            XQueryMerger merger = new XQueryMerger(source);
            merger.setMainQuery();
            File destinationFile = destinationRootPath.resolve(relativePath).toFile();
            getLog().debug("[Merger]    destination will be "+destinationFile.getAbsolutePath());
            try {
                String result = merger.merge();
                destinationFile.getParentFile().mkdirs();
                OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(destinationFile), merger.getEncoding());
                osw.write(result);
                osw.flush();
                osw.close();
                getLog().debug("[Merger] "+relativePath.toString()+" merged into "+destinationFile.getAbsolutePath());
            } catch (ParsingException ex) {
                getLog().error(ex.getMessage());
                throw new MojoExecutionException("Merge of "+sourceFile.getAbsolutePath()+" fails", ex);
            } catch (FileNotFoundException ex) {
                getLog().error(ex.getMessage());
                throw new MojoExecutionException("Unable to create destination "+destinationFile.getAbsolutePath(), ex);
            } catch (IOException ex) {
                getLog().error(ex.getMessage());
                throw new MojoExecutionException("While writing "+destinationFile.getAbsolutePath(), ex);
            }
        }
    }
    
    /**
     * Build the filter used to get files to merge
     * @return 
     */
    private IOFileFilter buildFilter() {
        String filterString = acceptFilter!=null ? acceptFilter : "*.xq";
        List<String> wildchars = new ArrayList<String>();
        if(filterString.contains(",")) {
            String[] filters = filterString.split(",");
            for(String filter: filters) {
                wildchars.add(filter.trim());
            }
        } else {
            wildchars.add(filterString);
        }
        return new WildcardFileFilter(wildchars);
    }
}

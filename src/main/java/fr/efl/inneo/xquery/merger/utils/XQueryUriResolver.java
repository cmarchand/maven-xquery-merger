/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.efl.inneo.xquery.merger.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

/**
 *
 * @author ext-cmarchand
 */
public class XQueryUriResolver implements URIResolver {

    private final URIResolver defaultUriResolver;

    /**
     * Unix double slash.
     */
    private static final String UNIX_DOUBLE_SLASH = "//";
    /**
     * Unix simple slash.
     */
    private static final String UNIX_SIMPLE_SLASH = "/";
    /**
     * Empty string.
     */
    private static final String EMPTY_STRING = "";
    /**
     * Unix file protocol.
     */
    private static final String UNIX_FILE_PROTOCOL = "file:/";
    /**
     * Windows file protocol.
     */
    private static final String WINDOWS_FILE_PROTOCOL = "file:\\";
    /**
     * uri mapping.
     */
    private final Map<String, String> uriMapping;
    
    public XQueryUriResolver() {
        this(new URIResolver() {
            @Override
            public Source resolve(String string, String string1) throws TransformerException {
                return null;
            }
        });
    }

    /**
     * @param defaultUriResolver Saxon default URIResolver
     * Default constructor.
     */
    public XQueryUriResolver(final URIResolver defaultUriResolver) {
        this(defaultUriResolver, Collections.EMPTY_MAP);
    }

    /**
     * Constructor with uri resolver mapping.
     *
     * @param defaultUriResolver Saxon default URIResolver
     * @param uriMapping the uri resolver mapping
     */
    public XQueryUriResolver(final URIResolver defaultUriResolver, Map<String, String> uriMapping) {
        super();
        this.defaultUriResolver = defaultUriResolver;
        this.uriMapping = new HashMap<>(uriMapping);
    }

    /**
     * {@inheritDoc}
     *
     * @param href
     * @param base
     * @return
     * @throws javax.xml.transform.TransformerException
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        try {
            if ("".equals(href)) {
                return defaultUriResolver.resolve(href, base);
            } else {
                String path = href;
                path = path.replace(UNIX_DOUBLE_SLASH, UNIX_SIMPLE_SLASH);
                path = path.replace(UNIX_FILE_PROTOCOL, UNIX_SIMPLE_SLASH);
                path = path.replace(WINDOWS_FILE_PROTOCOL, EMPTY_STRING);
                String filename = path.substring(path.lastIndexOf('/') + 1);
                File file = new File(path);
                if (uriMapping.containsKey(filename)) {
                    file = new File(uriMapping.get(filename));
                } else {
                    if (System.getProperty(filename) != null) {
                        file = new File(System.getProperty(filename));
                    } else {
                        if (!file.exists()) {
                            if (base == null || base.isEmpty()) {
                                path = href;
                            } else {
                                path = new File(base).getParent() + File.separator + href;
                            }
                            path = path.replace(UNIX_FILE_PROTOCOL, UNIX_SIMPLE_SLASH);
                            path = path.replace(WINDOWS_FILE_PROTOCOL, EMPTY_STRING);
                            file = new File(path);
                        }
                        if (!file.exists()) {
                            File localFile = new File(filename);
                            if (localFile.exists()) {
                                file = localFile;
                            } else {
                                File basedirFile = new File(System.getProperty("basedir"), filename);
                                if (basedirFile.exists()) {
                                    file = basedirFile;
                                }
                            }
                        }
                    }
                }
                if (file.exists()) {
                    StreamSource s = new StreamSource(new FileInputStream(file));
                    s.setSystemId(file);
                    return s;
                } else {
                    return null;
                }
            }
        } catch (FileNotFoundException e) {
            throw new TransformerException(e);
        }
    }
}

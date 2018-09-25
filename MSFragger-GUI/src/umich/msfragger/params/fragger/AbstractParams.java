/*
 * Copyright 2018 Dmitry Avtonomov.
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
package umich.msfragger.params.fragger;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import umich.msfragger.params.Props;

/**
 *
 * @author Dmitry Avtonomov
 */
public abstract class AbstractParams {
    
    protected Props props;

    public AbstractParams() {
        props = new Props();
    }

    public abstract Path tempFilePath();
    
    public abstract void loadDefault();

    public Props getProps() {
        return props;
    }
    
    /**
     * Loads properties either from the default properties file stored in the jar
     * or from the temp directory.
     * @throws IOException
     */
    public void load() throws IOException {
        // first check if there is a temp file saved
        Path tempFilePath = tempFilePath();
        if (Files.exists(tempFilePath)) {
            try (final FileInputStream fis = new FileInputStream(tempFilePath.toFile())) {
                load(fis, true);
            }
        } else {
            loadDefault();
        }
    }

    /**
     * Clear out the properties
     * @param is
     * @param clearBeforeLoading clear up the internal properties before loading new ones.
     * @throws IOException
     */
    public void load(InputStream is, boolean clearBeforeLoading) throws IOException {
        if (clearBeforeLoading) {
            clear();
        }
        props.load(is);
    }

    public void clear() {
        this.props.clearProps();
    }

    public void clearCache() {
        Path tempFilePath = tempFilePath();
        if (Files.exists(tempFilePath)) {
            try {
                Files.delete(tempFilePath);
            } catch (IOException ex) {
                // doesn't matter
            }
        }
    }

    /**
     * Saves the current properties contents to a default temp file.
     * @throws IOException
     */
    public Path save() throws IOException {
        Path temp = tempFilePath();
        if (Files.exists(temp)) {
            Files.delete(temp);
        }
        props.save(new FileOutputStream(temp.toFile()));
        return temp;
    }

    /**
     * Saves the current properties contents to a stream. With comments.
     * @param os
     * @throws IOException
     */
    public void save(OutputStream os) throws IOException {
        props.save(os);
    }
    
    protected int getInt(String name, String defaultVal) {
        return Integer.parseInt(props.getProp(name, defaultVal).value);
    }
    
    protected Integer getInt(String name) {
        Props.Prop prop = props.getProp(name);
        return prop == null ? null : Integer.parseInt(prop.value);
    }
    
    protected double getDouble(String name, String defaultVal) {
        return Double.parseDouble(props.getProp(name, defaultVal).value);
    }
    
    protected Double getDouble(String name) {
        Props.Prop prop = props.getProp(name);
        return prop == null ? null : Double.parseDouble(prop.value);
    }
    
    protected boolean getBoolean(String name, String defaultVal) {
        return Boolean.parseBoolean(props.getProp(name, defaultVal).value);
    }
    
    protected Boolean getBoolean(String name) {
        Props.Prop prop = props.getProp(name);
        return prop == null ? null : Boolean.parseBoolean(prop.value);
    }
    
    protected String getString(String name, String defaultVal) {
        return props.getProp(name, defaultVal).value;
    }
    
    protected String getString(String name) {
        Props.Prop prop = props.getProp(name);
        return prop == null ? null : prop.value;
    }
    
    public void setInt(String name, int val) {
        props.setProp(name, Integer.toString(val));
    }
    
    public void setInt(String name, String val) {
        Integer.parseInt(val);
        props.setProp(name, val);
    }
    
    public void setInt(String name, Integer val) {
        if (val == null)
            props.removeProp(name);
        else
            props.setProp(name, val.toString());
    }
    
    public void setDouble(String name, double val) {
        props.setProp(name, Double.toString(val));
    }
    
    public void setDouble(String name, String val) {
        Double.parseDouble(val);
        props.setProp(name, val);
    }
    
    public void setDouble(String name, Double val) {
        if (val == null)
            props.removeProp(name);
        else
            props.setProp(name, val.toString());
    }
    
    public void setString(String name, String val) {
        if (val == null)
            props.removeProp(name);
        else
            props.setProp(name, val);
    }
    
    public void setBool(String name, boolean val) {
        props.setProp(name, Boolean.toString(val));
    }
    
    public void setBool(String name, Boolean val) {
        if (val == null)
            props.removeProp(name);
        else
            props.setProp(name, Boolean.toString(val));
    }
    
    public void setBool(String name, String val) {
        Boolean.parseBoolean(val);
        props.setProp(name, val);
    }
}

package com.ly.fn.inf.lnk.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import com.ly.fn.inf.lnk.api.Address;
import com.ly.fn.inf.lnk.api.registry.RegistryModule;
import com.ly.fn.inf.lnk.remoting.protocol.JacksonSerializer;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月24日 下午4:11:12
 */
public class BasicInvoker {
    protected static final JacksonSerializer jacksonSerializer = new JacksonSerializer();
    static File file = new File("lookup.props");
    
    private static void saveLookup(String key, String value) {
        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream(file));
            properties.setProperty(key, value);
            properties.store(new FileOutputStream(file), "lnk lookup props");
            System.err.println("save " + key + " = " + value);
        } catch (FileNotFoundException e) {
            e.printStackTrace(System.err);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
    
    protected String getLookup(String key) {
        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream(file));
            String property = properties.getProperty(key);
            System.err.println("get " + key + " = " + property);
            return property;
        } catch (FileNotFoundException e) {
            e.printStackTrace(System.err);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        return null;
    }
    
    
    protected static final RegistryModule registryModule = new RegistryModule() {
        @Override
        public void registry(String serviceGroup, String serviceId, int version, int protocol, Address addr) {
            saveLookup(serviceGroup + serviceId + version + protocol, addr.toString());
        }

        @Override
        public void unregistry(String serviceGroup, String serviceId, int version, int protocol) {}
    };
}

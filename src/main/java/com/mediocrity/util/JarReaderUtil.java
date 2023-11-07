package com.mediocrity.util;

import com.mediocrity.SinkFinder;
import com.mediocrity.entity.Rules;
import com.mediocrity.model.ClassInfo;
import com.mediocrity.model.ClassRepo;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * @author: medi0cr1ty
 * @date: 2023/10/13
 */
@Slf4j
public class JarReaderUtil {
    private static final Logger logger = LoggerFactory.getLogger(JarReaderUtil.class);

    /**
     * 读取 Class文件并转换为 ClassNode.
     *
     * @param jar         The jar file with the classes.
     * @param ruls
//     * @param isSingleJar
     */
    public static void readJar(File jar, Rules ruls) {
        String[] names = jar.getName().split("\\\\");
        String name = names[names.length - 1];

        if (RuleUtil.isExcluded(name, ruls.getJarNameExclusions())) {
            return;
        }

        if ((jar.getName().endsWith(".class") || jar.getName().endsWith(".class/")) && !RuleUtil.isExcluded(jar.getName().replaceAll("/", "\\."), ruls.getClassExclusions())) {
            try (final InputStream fis = new FileInputStream(jar)) {
                streamToNode(fis, jar.getPath());
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (RuleUtil.isIncluded(name, ruls.getJarNameInclusions())) {
            logger.info(jar.getName());
            try (final JarInputStream jis = new JarInputStream(new ByteArrayInputStream(Files.readAllBytes(jar.toPath())))) {

                JarEntry jarEntry;
                while ((jarEntry = jis.getNextJarEntry()) != null) {
                    String itemName = jarEntry.getName();

                    if (itemName.endsWith(".class") || itemName.endsWith(".class/")) {
                        if (RuleUtil.isExcluded(itemName.replaceAll("/", "\\."), ruls.getClassExclusions())) {
                            continue;
                        }
                        streamToNode(jis, jar.getPath());
                    }
                    if (itemName.endsWith(".jar")) {
                        readJar(jar.getName().substring(0, jar.getName().lastIndexOf(".")), jis, ruls);
                    }
                }
            } catch (Exception e) {
                if (e instanceof ZipException &&
                        e.getMessage().contains("invalid entry CRC")) {
                    try (ZipFile zf = new ZipFile(jar)) {
                        final Enumeration<? extends ZipEntry> entries = zf.entries();
                        while (entries.hasMoreElements()) {
                            final ZipEntry entry = entries.nextElement();
                            final String itemName = entry.getName();

                            if (itemName.endsWith(".class") || itemName.endsWith(".class/")) {
                                if (RuleUtil.isExcluded(itemName.replaceAll("/", "\\."), ruls.getClassExclusions())) {
                                    continue;
                                }
                                try (final InputStream zis = zf.getInputStream(entry)) {
                                    streamToNode(zis, jar.getPath());
                                }
                            }
                        }
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }
            }
        }
    }

    private static void readJar(String name, JarInputStream jis, Rules ruls) throws IOException {

        JarEntry jarEntry;
        while ((jarEntry = jis.getNextJarEntry()) != null) {
            String itemName = jarEntry.getName().split("/")[jarEntry.getName().split("/").length-1];
            if ( !RuleUtil.isExcluded(itemName, ruls.getJarNameExclusions()) && RuleUtil.isIncluded(itemName,
                    ruls.getJarNameInclusions()) ) {
                File tempFile = File.createTempFile(name + "@", itemName);

                jarInputStreamToFile(jis, tempFile);

                readJar(tempFile, ruls);
            }
        }
    }

    private static void jarInputStreamToFile(JarInputStream jarInputStream, File outputFile) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        final FileOutputStream outputStream = new FileOutputStream(outputFile);

        while ((bytesRead = jarInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        outputStream.close();

    }

    /**
     * 根据输入流转换为 ClassNode
     *
     * @param is The inputstream convert to be classnode.
     */
    private static void streamToNode(InputStream is, String jarName) {
        try {
            final ByteArrayOutputStream streamBuilder = new ByteArrayOutputStream();
            int bytesRead;
            final byte[] tempBuffer = new byte[32767];

            while ((bytesRead = is.read(tempBuffer)) != -1) {
                streamBuilder.write(tempBuffer, 0, bytesRead);
            }

            final ClassNode node = new ClassNode();
            new ClassReader(streamBuilder.toByteArray()).accept(node, ClassReader.SKIP_FRAMES);

            if (jarName.indexOf("@") > 0){
                jarName = jarName.replaceAll("@\\d+", "@");
            }
            jarName = jarName.replace(SinkFinder.TARGET_PATH,"${PATH}");

            final ClassInfo classInfo = new ClassInfo(node, jarName);
            ClassRepo.classes.put(node.name.replaceAll("/","\\."), classInfo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pulsar.admin.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import org.apache.pulsar.admin.cli.utils.SchemaExtractor;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.common.protocol.schema.PostSchemaPayload;

@Parameters(commandDescription = "Operations about schemas")
public class CmdSchemas extends CmdBase {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public CmdSchemas(PulsarAdmin admin) {
        super("schemas", admin);
        jcommander.addCommand("get", new GetSchema());
        jcommander.addCommand("delete", new DeleteSchema());
        jcommander.addCommand("upload", new UploadSchema());
        jcommander.addCommand("extract", new ExtractSchema());
    }

    @Parameters(commandDescription = "Get the schema for a topic")
    private class GetSchema extends CliCommand {
        @Parameter(description = "persistent://tenant/namespace/topic", required = true)
        private java.util.List<String> params;

        @Parameter(names = { "--version" }, description = "version", required = false)
        private Long version;

        @Override
        void run() throws Exception {
            String topic = validateTopicName(params);
            if (version == null) {
                System.out.println(admin.schemas().getSchemaInfoWithVersion(topic));
            } else {
                System.out.println(admin.schemas().getSchemaInfo(topic, version));
            }
        }
    }

    @Parameters(commandDescription = "Delete the latest schema for a topic")
    private class DeleteSchema extends CliCommand {
        @Parameter(description = "persistent://tenant/namespace/topic", required = true)
        private java.util.List<String> params;

        @Override
        void run() throws Exception {
            String topic = validateTopicName(params);
            admin.schemas().deleteSchema(topic);
        }
    }

    @Parameters(commandDescription = "Update the schema for a topic")
    private class UploadSchema extends CliCommand {
        @Parameter(description = "persistent://tenant/namespace/topic", required = true)
        private java.util.List<String> params;

        @Parameter(names = { "-f", "--filename" }, description = "filename", required = true)
        private String schemaFileName;

        @Override
        void run() throws Exception {
            String topic = validateTopicName(params);
            PostSchemaPayload input = MAPPER.readValue(new File(schemaFileName), PostSchemaPayload.class);
            admin.schemas().createSchema(topic, input);
        }
    }

    @Parameters(commandDescription = "Provide the schema via a topic")
    private class ExtractSchema extends CliCommand {
        @Parameter(description = "persistent://tenant/namespace/topic", required = true)
        private java.util.List<String> params;

        @Parameter(names = { "-j", "--jar" }, description = "jar filepath", required = true)
        private String jarFilePath;

        @Parameter(names = { "-t", "--type" }, description = "type avro or json", required = true)
        private String type;

        @Parameter(names = { "-c", "--classname" }, description = "class name of pojo", required = true)
        private String className;

        @Override
        void run() throws Exception {
            String topic = validateTopicName(params);

            File file  = new File(jarFilePath);
            ClassLoader cl = new URLClassLoader(new URL[]{ file.toURI().toURL() });

            Class cls = cl.loadClass(className);

            PostSchemaPayload input = new PostSchemaPayload();

            if (type.toLowerCase().equalsIgnoreCase("avro")) {
                input.setType("AVRO");
                input.setSchema(SchemaExtractor.getAvroSchemaInfo(cls));
            } else if (type.toLowerCase().equalsIgnoreCase("json")){
                input.setType("JSON");
                input.setSchema(SchemaExtractor.getJsonSchemaInfo(cls));
            }
            else {
                throw new Exception("Unknown schema type specified as type");
            }

            admin.schemas().createSchema(topic, input);
        }
    }

}
package rocks.inspectit.ocelot.config.loaders;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigFileLoaderTest {

    private String[][] getDefaultConfigTestData() {
        String currentWorkingDir = System.getProperty("user.dir");
        StringBuilder builder = new StringBuilder()
                .append("file [").append(currentWorkingDir)
                .append("\\build\\resources\\main\\rocks\\inspectit\\ocelot\\config\\default\\basics.yml]");
        String key1 = builder.toString();
        String value1 = "inspectit:\n" +
                "\n" +
                "  # the name of the service which is being instrumented\n" +
                "  service-name: \"InspectIT Agent\"\n" +
                "\n" +
                "  # defines common tags to be be set on the metrics\n" +
                "  tags:\n" +
                "    # different tag providers that can be configured\n" +
                "    providers:\n" +
                "      # environment provider adds 'service-name', 'host' and 'host-address' tags\n" +
                "      environment:\n" +
                "        # if environment provider is enabled\n" +
                "        enabled: true\n" +
                "        # should the host name be resolved using InetAddress.getLocalHost(), if false 'host' tag is not added by env provider\n" +
                "        resolve-host-name: true\n" +
                "        # should the host address be resolved using InetAddress.getLocalHost(), if false 'host-address' tag is not added by env provider\n" +
                "        resolve-host-address: true\n" +
                "\n" +
                "    # specifies user defined tag keys and values as a map\n" +
                "    # these tag values would overwrite any value added by the providers, thus you can easily overwrite tags values by your own\n" +
                "    extra: {}\n" +
                "\n" +
                "  # general settings regarding trace capturing\n" +
                "  tracing:\n" +
                "    # master switch for trace capturing. When set to false the following happens:\n" +
                "    #  - all trace exporters are disabled\n" +
                "    #  - tracing is disabled for all instrumentation rules\n" +
                "    enabled: true\n" +
                "\n" +
                "    # global sample probability used to decide if a trace shall be sampled or not\n" +
                "    # this value can be overridden by the tracing settings of individual instrumentation rules.\n" +
                "    sample-probability: 1.0\n" +
                "\n" +
                "  # general settings regarding metrics capturing\n" +
                "  metrics:\n" +
                "    # master switch for metrics capturing. When set to false the following happens:\n" +
                "    #  - all metrics exporters are disabled\n" +
                "    #  - all metrics recorders are disabled\n" +
                "    #  - no measurement values are collected via instrumentation, however the instrumentation is still performed\n" +
                "    #  - no views and measures are created\n" +
                "    enabled: true\n" +
                "\n" +
                "  # logging settings\n" +
                "  logging:\n" +
                "      # path to a custom user-specified logback config file that should be used\n" +
                "      config-file:\n" +
                "      # properties below only work if the default inspectIT Ocelot logback config file is used\n" +
                "      # sets the inspectIT Ocelot log level to TRACE\n" +
                "      trace: false\n" +
                "      # sets the level to DEBUG (only if trace is false)\n" +
                "      debug: false\n" +
                "      # settings for the console output\n" +
                "      console:\n" +
                "        # defines if the console output is enabled\n" +
                "        enabled: true\n" +
                "        # defines a custom pattern to output to the console\n" +
                "        pattern:\n" +
                "      # settings for the file-based log output\n" +
                "      # inspectIT Ocelot will create two log files: agent.log and exceptions.log\n" +
                "      file:\n" +
                "        # defines if the file-based log output is enabled\n" +
                "        enabled: true\n" +
                "        # defines a custom pattern to output to the console\n" +
                "        pattern:\n" +
                "        # defines a custom path where log files should be placed (defaults to /tmp/inspectit-oce)\n" +
                "        path:\n" +
                "        # if the default pattern should include the service name (specified with inspectit.service-name)\n" +
                "        # helpful when you run more than one service on the same host\n" +
                "        include-service-name: true\n" +
                "\n" +
                "  # defines how many threads inspectIT may start for its internal tasks\n" +
                "  thread-pool-size: 2";
        builder = new StringBuilder()
                .append("file [").append(currentWorkingDir)
                .append("\\build\\resources\\main\\rocks\\inspectit\\ocelot\\config\\default\\instrumentation\\propagation\\propagation-http-apacheclient.yml]");
        String key2 = builder.toString();
        String value2 = "inspectit:\n" +
                "  instrumentation:\n" +
                "      \n" +
                "    data:\n" +
                "\n" +
                "      apache_http_client_propagation_performed: {is-tag: false}\n" +
                "      apache_http_client_propagation_is_entry: {is-tag: false, down-propagation: NONE}\n" +
                "\n" +
                "    actions:\n" +
                "      apache_http_client_down_propagation:\n" +
                "        is-void: true\n" +
                "        imports:\n" +
                "          - java.util\n" +
                "          - org.apache.http\n" +
                "        input:\n" +
                "          _arg1: HttpMessage\n" +
                "          _context: InspectitContext\n" +
                "        value-body: |\n" +
                "          Map headers = _context.getDownPropagationHeaders();\n" +
                "          Iterator it = headers.entrySet().iterator();\n" +
                "          while(it.hasNext()) {\n" +
                "            Map$Entry e = (Map$Entry) it.next();\n" +
                "            _arg1.setHeader((String) e.getKey(), (String) e.getValue());\n" +
                "          }\n" +
                "\n" +
                "      apache_http_client_up_propagation:\n" +
                "        is-void: true\n" +
                "        imports:\n" +
                "          - java.util\n" +
                "          - org.apache.http\n" +
                "        input:\n" +
                "          _returnValue: HttpMessage\n" +
                "          _context: InspectitContext\n" +
                "        value-body: |\n" +
                "          if(_returnValue != null) {\n" +
                "            Collection headerKeys = _context.getPropagationHeaderNames();\n" +
                "            Map presentHeaders = new HashMap();\n" +
                "            Iterator it = headerKeys.iterator();\n" +
                "            while(it.hasNext()) {\n" +
                "              String name = (String) it.next();\n" +
                "              Header[] headers = _returnValue.getHeaders(name);\n" +
                "              if (headers != null && headers.length > 0) {\n" +
                "                StringBuilder sb = new StringBuilder();\n" +
                "                for(int i = 0; i< headers.length; i++) {\n" +
                "                  String value = headers[i].getValue();\n" +
                "                  if(value != null) {\n" +
                "                    if(sb.length() > 0) {\n" +
                "                      sb.append(',');\n" +
                "                    }\n" +
                "                    sb.append(value);\n" +
                "                  }\n" +
                "                }\n" +
                "                presentHeaders.put(name, sb.toString());\n" +
                "              }\n" +
                "            }\n" +
                "            _context.readUpPropagationHeaders(presentHeaders);\n" +
                "          }\n" +
                "\n" +
                "    rules:\n" +
                "      http_propagation_apache_doExecute:\n" +
                "        scopes:\n" +
                "          apache_http_client_doExecute: true\n" +
                "\n" +
                "        post-entry:\n" +
                "          apache_http_client_propagation_is_entry:\n" +
                "            action: test_and_set_marker\n" +
                "            constant-input: { marker: apache_http_client_propagation_performed}\n" +
                "          do_down_propagation:\n" +
                "            action: apache_http_client_down_propagation\n" +
                "            only-if-true: apache_http_client_propagation_is_entry\n" +
                "          \n" +
                "        pre-exit:\n" +
                "          do_up_propagation:\n" +
                "            action: apache_http_client_up_propagation\n" +
                "            only-if-true: apache_http_client_propagation_is_entry\n";
        String[] keyValuePair1 = {key1, value1};
        String[] keyValuePair2 = {key2, value2};
        String[][] defaultConfigTestData = {keyValuePair1, keyValuePair2};
        return defaultConfigTestData;
    }

    private Map<String, String> getFallbackConfigTestData() {
        String currentWorkingDir = System.getProperty("user.dir");
        StringBuilder builder = new StringBuilder()
                .append("file [").append(currentWorkingDir)
                .append("\\build\\resources\\main\\rocks\\inspectit\\ocelot\\config\\fallback\\disable-default-instrumentations.yml]");
        String key1 = builder.toString();
        String value1 = "inspectit:\n" +
                "\n" +
                "  instrumentation:\n" +
                "\n" +
                "    rules:\n" +
                "      servicegraph_record_apache_client: {enabled: false }\n" +
                "      servicegraph_record_jdbc_calls: {enabled: false }\n" +
                "      servicegraph_record_servletapi_entry: {enabled: false }\n" +
                "\n" +
                "      http_client_apache_client: {enabled: false }\n" +
                "      http_server_servlet_api: {enabled: false }\n" +
                "\n" +
                "      http_propagation_apache_doExecute: {enabled: false}\n" +
                "      httpurlconnection_down_propagation: {enabled: false}\n" +
                "      httpurlconnection_up_propagation: {enabled: false}\n" +
                "\n" +
                "      servlet_api_down_propagation: {enabled: false}\n" +
                "      servlet_api_up_propagation_on_servlet_and_filter: {enabled: false}\n" +
                "      servlet_api_up_propagation_on_servletresponse: {enabled: false}\n\n";
        builder = new StringBuilder()
                .append("file [").append(currentWorkingDir)
                .append("\\build\\resources\\main\\rocks\\inspectit\\ocelot\\config\\fallback\\fallback.yml]");
        String key2 = builder.toString();
        String value2 = "#------------------------\n" +
                "# This file contains fallback configuration overwrites in case the agent is started with an invalid configuration\n" +
                "# In this case, the agent tries to preserve the inspecit.config settings and to attach them to the fallback configuration\n" +
                "# to make sure that runtime configuration updated are possible without a restart\n" +
                "#------------------------\n" +
                "inspectit:\n" +
                "  instrumentation:\n" +
                "    # settings for special sensors\n" +
                "    special:\n" +
                "      executor-context-propagation: false\n" +
                "      scheduled-executor-context-propagation: false\n" +
                "      thread-start-context-propagation: false\n" +
                "      class-loader-delegation: false\n" +
                "  metrics:\n" +
                "    enabled: false\n" +
                "  tracing:\n" +
                "    enabled: false\n" +
                "\n" +
                "  self-monitoring:\n" +
                "    enabled: false";
        Map<String, String> fallBackConfigTestData = new HashMap<>();
        fallBackConfigTestData.put(key1, value1);
        fallBackConfigTestData.put(key2, value2);
        return fallBackConfigTestData;
    }

    @Nested
    public class GetDefaultConfig {
        @Test
        void GetDefaultConfig() throws IOException {
            String[][] sampleSets = getDefaultConfigTestData();

            Map<String, String> output = ConfigFileLoader.getDefaultConfigFiles();

            assertThat(output.size()).isEqualTo(25);
            assertThat(output.get(sampleSets[0][0])).isEqualTo(sampleSets[0][1]);
            assertThat(output.get(sampleSets[1][0])).isEqualTo(sampleSets[1][1]);
        }
    }

    @Nested
    public class GetFallBackConfig {
        @Test
        void getFallbackConfig() throws IOException {
            Map<String, String> expected = getFallbackConfigTestData();

            Map<String, String> output = ConfigFileLoader.getFallbackConfigFiles();

            assertThat(output).isEqualTo(expected);
        }
    }
}
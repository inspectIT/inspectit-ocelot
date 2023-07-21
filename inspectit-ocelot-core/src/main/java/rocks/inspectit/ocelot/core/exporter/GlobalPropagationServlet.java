package rocks.inspectit.ocelot.core.exporter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

@Slf4j
public class GlobalPropagationServlet extends HttpServlet {

    private final ObjectMapper mapper;

    private final GlobalPropagationDataStorage dataStorage;

    public GlobalPropagationServlet() {
        this.mapper = new ObjectMapper();
        this.dataStorage = GlobalPropagationDataStorage.getInstance();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);

        response.setHeader("Access-Control-Allow-Origin", "*"); // Replace * with specific origin
        response.setHeader("Access-Control-Allow-Methods", "GET, PUT");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Credentials", "true");

        String res = mapper.writeValueAsString(this.dataStorage.readData());
        response.getWriter().write(res);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> newPropagationData = this.getRequestBody(request);
        this.dataStorage.writeData(newPropagationData);

        response.setStatus(HttpServletResponse.SC_OK);
    }

    private Map<String, Object> getRequestBody(HttpServletRequest request) throws IOException {
        try (BufferedReader reader = request.getReader()) {
            return mapper.readValue(reader, new TypeReference<Map<String,Object>>() {});
        }
    }
}

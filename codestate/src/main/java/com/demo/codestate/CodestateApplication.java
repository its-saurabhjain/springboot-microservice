package com.demo.codestate;

import brave.Tracing;
import brave.opentracing.BraveTracer;
import com.google.gson.JsonObject;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.uber.jaeger.Configuration;
import com.uber.jaeger.samplers.ProbabilisticSampler;
import zipkin.Span;
import zipkin.reporter.AsyncReporter;
import zipkin.reporter.okhttp3.OkHttpSender;

import java.util.Arrays;
import java.util.List;


@SpringBootApplication
@RestController
public class CodestateApplication {

    public static final String serverUrlLocal = "http://localhost:9090";
    //public static final String serverUrl = "http://dataservice.default.svc.cluster.local";
    //public static final String serverUrl = "http://dataservice-k8sdemo.172.17.205.46.nip.io";

    public static void main(String[] args) {
        SpringApplication.run(CodestateApplication.class, args);
    }

    public static String requestProcessedData(String url){
        RestTemplate request = new RestTemplate();
        String result = request.getForObject(url,String.class);
        System.out.print(url);
        return (result);
    }
    
    @GetMapping("/")
    public static String Hello(){
        //return new ResponseEntity(null, status);
        return "I'M YOUR CONVERTOR";
    }

    @GetMapping("/codeToState")
    public static String CodeToState(@RequestParam("code") String code){

        String state = null;
        String serverUrl = System.getenv().getOrDefault("DATA_SERVICE_URL", serverUrlLocal);
        try {
            String response = requestProcessedData(serverUrl+"/readDataForCode");
            JSONObject jsonObject = new JSONObject(response);
            state = jsonObject.getString(code.toUpperCase());
        } catch (Exception e) {
            System.out.println("[ERROR] : [CUSTOM_LOG] : " + e);
        }

        if(state == null){
            state = "No Match Found";
        }
        return state;
    }
    @GetMapping("/stateToCode")
    public static String StateToCode(@RequestParam("state") String state){
        String value = "";
        String serverUrl = System.getenv().getOrDefault("DATA_SERVICE_URL", serverUrlLocal);
        try {
            String response = requestProcessedData(serverUrl+"/readDataForState");
            JSONArray jsonArray = new JSONArray(response);

            for(int n = 0; n < jsonArray.length(); n++)
            {
                JSONObject object = jsonArray.getJSONObject(n);
                String name = object.getString("name");

                if(state.equalsIgnoreCase(name)){
                    value = object.getString("abbreviation");
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[ERROR] : [CUSTOM_LOG] : " + e);
        }

        if(value == null){
            value = "No Match Found";
        }
        return value;
    }

    @GetMapping("/test")
    public static String TestBkEnd(){
        String serverUrl = System.getenv().getOrDefault("DATA_SERVICE_URL", serverUrlLocal);
        String response = serverUrl;
        try {
            response = requestProcessedData(serverUrl + "/backend1");
        }
        catch(Exception ex){
        }
        return response;

    }
    //@Bean
    public io.opentracing.Tracer jaegerTracer() {
        return new Configuration("CodeState-FrontEnd", new Configuration.SamplerConfiguration(ProbabilisticSampler.TYPE, 1),
                new Configuration.ReporterConfiguration())
                .getTracer();
    }
    //@Bean
    public io.opentracing.Tracer zipkinTracer() {
        OkHttpSender okHttpSender = OkHttpSender.create("http://localhost:9411/api/v1/spans");
        AsyncReporter<Span> reporter = AsyncReporter.builder(okHttpSender).build();
        Tracing braveTracer = Tracing.newBuilder().localServiceName("spring-boot").reporter(reporter).build();
        return BraveTracer.create(braveTracer);
    }

}

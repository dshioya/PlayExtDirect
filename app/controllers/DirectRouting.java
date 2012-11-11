package controllers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.JsonNode;

import play.Play;
import play.data.Form;
import play.data.validation.Constraints.Required;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import direct.Direct.TYPE;
import direct.DirectClass;
import direct.DirectFormParam;
import direct.DirectHttpParam;
import direct.DirectMethod;
import direct.DirectUpload;

/**
 * ExtDirectルーティングコントローラクラス。
 */
public class DirectRouting extends Controller {

    /**
     * ダイレクトの名前空間。
     */
    private static final String NAMESPACE = "Sample.direct";

    /**
     * ダイレクトクラスのパッケージ名。
     */
    private static final String DIRECT_PACKAGE = "direct";

    @SuppressWarnings("rawtypes")
    private static final Map<String, Class[]> PARAMETER_TYPE_MAP = new HashMap<String, Class[]>();

    public static StringBuilder PROVIDER_API = null;

    /**
     * ExtDirectリモートコールのルーティングアクション。
     * @return 処理結果
     * @throws Exception
     */
    public static Result call() {

        Form<FormPost> fpForm = form(FormPost.class).bindFromRequest();

        if(!fpForm.hasErrors()) {

            try {
                return procFormPost(fpForm.get());
            } catch (Exception e) {
                return ok(formatException(e));
            }
        }

        JsonNode data = request().body().asJson();
        JsonNode firstNode = data.get(0);

        if(firstNode == null) {
            try {
                return ok(procHttpPost(data));
            } catch (Exception e) {
                return ok(formatException(e));
            }
        } else {

            List<JsonNode> jsonList = new ArrayList<JsonNode>();

            for(int n = 0; n < data.size(); n++) {
                JsonNode subData = data.get(n);

                try {
                    jsonList.add(procHttpPost(subData));
                } catch (Exception e) {
                    jsonList.add(formatException(e));
                }
            }

            return ok(jsonList.toString());
        }

    }

    /**
     * HTTP Postを処理する。
     * @param hp
     * @return 処理結果
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static JsonNode procHttpPost(JsonNode data) throws Exception {

        String strAction = data.get("action").asText();
        String strMethod = data.get("method").asText();
        String tid = data.get("tid").asText();
        JsonNode dataNode = data.get("data");

        Class[] parameters = PARAMETER_TYPE_MAP.get(strAction + "_" + strMethod);

        // リモートコール対象の処理を実行
        Class directClass = Class.forName(DIRECT_PACKAGE + "." + strAction);
        Object directObject = directClass.newInstance();
        Method method = directClass.getMethod(strMethod, parameters);
        Object result = null;

        if(dataNode.size() > 0) {

            JsonNode innerData = dataNode.get(0);
            List<String> dataList = new ArrayList<String>();

            if(innerData.size() > 0) {
                Iterator iter = innerData.iterator();
                while(iter.hasNext()) {
                    dataList.add(iter.next().toString());
                }
            } else {
                for(int n = 0; n < dataNode.size(); n++) {
                    dataList.add(dataNode.get(n).asText());
                }
            }

            Field[] fields = directClass.getFields();

            for (Field field : fields) {
                DirectHttpParam dh = field.getAnnotation(DirectHttpParam.class);

                if(dh != null) {
                    // リクエストパラメータをバインド
                    field.set(directObject, dataList);
                }
            }
        }

        result = method.invoke(directObject);

        // 処理結果を構築
        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put("type", "rpc");
        resultMap.put("tid", tid);
        resultMap.put("action", strAction);
        resultMap.put("method", strMethod);
        resultMap.put("result", result);

        return Json.toJson(resultMap);
    }

    /**
     * FORM Postを処理する。
     * @param fp
     * @return 処理結果
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Result procFormPost(FormPost fp) throws Exception {

        Map<String, String[]> params = null;
        List<FilePart> fileParts = null;

        if(fp.extUpload) {
            MultipartFormData mdata = request().body().asMultipartFormData();

            // アップロードファイルを取得
            fileParts = mdata.getFiles();

            params = mdata.asFormUrlEncoded();
        } else {
            params = request().body().asFormUrlEncoded();
        }

        Map<String, String> requestParams = new HashMap<String, String>();

        // Form Postのキーを除いたパラメータセットを作成
        for(String key : params.keySet()) {
            if(!isFormPostKey(key)) {
                String[] values = params.get(key);

                if(values.length == 1) {
                    requestParams.put(key, values[0]);
                } else {
                    for(int n = 0; n < values.length; n++) {
                        requestParams.put(key + "[" + n + "]", values[n]);
                    }
                }
            }
        }

        // リモートコール対象の処理を実行
        Class directClass = Class.forName(DIRECT_PACKAGE + "." + fp.extAction);
        Object directObject = directClass.newInstance();
        Method method = directClass.getMethod(fp.extMethod, PARAMETER_TYPE_MAP.get(fp.extAction + "_" + fp.extMethod));

        Field[] fields = directClass.getFields();

        for (Field field : fields) {

            if(fp.extUpload) {
                DirectUpload du = field.getAnnotation(DirectUpload.class);

                if(du != null) {
                    // アップロードファイルをバインド
                    field.set(directObject, fileParts);
                }
            }

            DirectFormParam dp = field.getAnnotation(DirectFormParam.class);

            if(dp != null) {
                // リクエストパラメータをバインド
                field.set(directObject, requestParams);
            }
        }

        Object result = method.invoke(directObject);

        // 処理結果を構築
        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put("type", "rpc");
        resultMap.put("tid", fp.extTID);
        resultMap.put("action", fp.extAction);
        resultMap.put("method", fp.extMethod);
        resultMap.put("result", result);

        if(fp.extUpload) {
            return ok(Json.toJson(resultMap)).as("text/html");
        } else {
            return ok(Json.toJson(resultMap));
        }
    }

    private static JsonNode formatException(Exception e) {

        Map<String, Object> result = new HashMap<String, Object>();
        Throwable cause = e.getCause();

        result.put("type", "exception");

        if(cause != null) {
            result.put("message", cause.getMessage());
            result.put("where", cause.getStackTrace());
        } else {
            result.put("message", e.getMessage());
            result.put("where", e.getStackTrace());
        }

        return Json.toJson(result);
    }

    /**
     * Form Postのキー項目であるか判定する。
     * @param key 判定する項目
     * @return キー項目の場合true
     */
    private static boolean isFormPostKey(String key) {
        return "extTID".equals(key) || "extAction".equals(key) || "extMethod".equals(key) || "extType".equals(key) || "extUpload".equals(key);
    }

    /**
     * PROVIDER_APIを返す。
     * @return PROVIDER_API
     */
    public static Result api() {

        if(PROVIDER_API == null) {
            defineProvider();
        }

        return ok(PROVIDER_API.toString()).as("application/json");
    }

    /**
     * PROVIDER_APIを定義する。
     */
    @SuppressWarnings("rawtypes")
    private static void defineProvider() {

        PROVIDER_API = new StringBuilder();

        Map<String, Object> providerMap = new HashMap<String, Object>();
        providerMap.put("url", "/direct/call");
        providerMap.put("type", "remoting");

        Map<String, Object> actionMap = new HashMap<String, Object>();
        providerMap.put("actions", actionMap);

        if(NAMESPACE != null && NAMESPACE.length() > 0) {
            providerMap.put("namespace", NAMESPACE);
        }

        Set<String> directClassNames = Play.application().getTypesAnnotatedWith(DIRECT_PACKAGE, DirectClass.class);

        for (String klass: directClassNames) {

            String className = klass.replaceFirst(DIRECT_PACKAGE + "\\.", "");

            List<Map<String, Object>> methodList = new ArrayList<Map<String,Object>>();

            actionMap.put(className, methodList);

            try {
                Method[] methods = Class.forName(klass).getMethods();

                for (Method method : methods) {
                    Map<String, Object> methodMap = new HashMap<String, Object>();

                    DirectMethod dm = method.getAnnotation(DirectMethod.class);

                    if(dm != null) {

                        Class[] parameterTypes = method.getParameterTypes();
                        String methodName = method.getName();

                        if(dm.type() == TYPE.HTTP_POST) {
                            PARAMETER_TYPE_MAP.put(className + "_" + methodName, parameterTypes);
                        }

                        methodMap.put("name", methodName);
                        methodMap.put("len", dm.length());

                        if(dm.type() == TYPE.FORM_POST) {
                            methodMap.put("formHandler", true);
                        }

                        methodList.add(methodMap);
                    }
                }

            } catch (ClassNotFoundException e) {}

        }

        PROVIDER_API.append("Ext.app.REMOTING_API=").append(Json.toJson(providerMap).toString());
    }

    /**
     * HTTP Postパラメータモデルクラス。
     */
    public static class HttpPost {

        @Required
        public String action;

        @Required
        public String method;

        @SuppressWarnings("rawtypes")
        public List data;

        @Required
        public String type;

        @Required
        public Integer tid;
    }

    /**
     * FORM Postパラメータモデルクラス。
     */
    public static class FormPost {

        @Required
        public Integer extTID;

        @Required
        public String extAction;

        @Required
        public String extMethod;

        @Required
        public String extType;

        public Boolean extUpload = false;

    }

}

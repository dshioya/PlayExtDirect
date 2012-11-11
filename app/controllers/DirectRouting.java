package controllers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jackson.JsonNode;

import play.Play;
import play.api.data.FormUtils;
import play.data.Form;
import play.data.validation.Constraints.Required;
import play.libs.Json;
import play.libs.Scala;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import direct.Direct.TYPE;
import direct.DirectClass;
import direct.DirectFormParam;
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

    /**
     * リクエスト解析パターン。
     */
    private static final Pattern KEY_PATTERN = Pattern.compile("^\\[(\\d+)\\]\\.(.+)");

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

        Form<HttpPost> hpForm = form(HttpPost.class).bindFromRequest();

        if(!hpForm.hasErrors()) {
            try {
                return ok(procHttpPost(hpForm.get()));
            } catch (Exception e) {
                return ok(formatException(e));
            }
        } else {

            List<Form<HttpPost>> httpPostList = parseMultiRequest();
            List<JsonNode> jsonList = new ArrayList<JsonNode>();

            for (Form<HttpPost> form : httpPostList) {

                if(form.hasErrors()) {
                    jsonList.add(formatException(form));
                } else {
                    try {
                        jsonList.add(procHttpPost(form.get()));
                    } catch (Exception e) {
                        jsonList.add(formatException(e));
                    }
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
    private static JsonNode procHttpPost(HttpPost hp) throws Exception {

        // リモートコール対象の処理を実行
        Class directClass = Class.forName(DIRECT_PACKAGE + "." + hp.action);
        Object directObject = directClass.newInstance();
        Method method = directClass.getMethod(hp.method, PARAMETER_TYPE_MAP.get(hp.action + "_" + hp.method));
        Object result = method.invoke(directObject, hp.data.toArray());

        // 処理結果を構築
        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put("type", "rpc");
        resultMap.put("tid", hp.tid);
        resultMap.put("action", hp.action);
        resultMap.put("method", hp.method);
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

    private static <T> JsonNode formatException(Form<T> form) {

        Map<String, Object> result = new HashMap<String, Object>();

        result.put("type", "exception");
        result.put("message", form.errorsAsJson());
        result.put("where", "parameter parse");

        return Json.toJson(result);
    }

    private static JsonNode formatException(Exception e) {

        Map<String, Object> result = new HashMap<String, Object>();

        result.put("type", "exception");
        result.put("message", e.getCause().getMessage());
        result.put("where", e.getCause().getStackTrace());

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
                        methodMap.put("len", parameterTypes.length);

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
     * 配列パラメータを解析する。
     * @return 解析結果
     */
    @SuppressWarnings("rawtypes")
    private static List<Form<HttpPost>> parseMultiRequest() {

        Map<String, Map<String, String>> dataMap = new HashMap<String, Map<String,String>>();
        Map rdata = requestData(ctx());
        Map<String, String> data = null;

        for(Object k : rdata.keySet()) {
            String key = (String) k;
            String value = (String)rdata.get(key);

            Matcher m = KEY_PATTERN.matcher(key);

            if(m.find()) {
                String strIndex = m.group(1);
                String item = m.group(2);

                if(dataMap.containsKey(strIndex)) {
                    data = dataMap.get(strIndex);
                } else {
                    data = new HashMap<String, String>();
                    dataMap.put(strIndex, data);
                }

                data.put(item, value);
            }
        }

        List<Form<HttpPost>> httpPostList = new ArrayList<Form<HttpPost>>();

        for(String key: dataMap.keySet()) {
            data = dataMap.get(key);

            Form<HttpPost> hpForm = form(HttpPost.class).bind(data);

            httpPostList.add(hpForm);
        }

        return httpPostList;
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
        @Required
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

    /**
     * リクエストデータをマップ形式で返す。
     * @param ctx コンテキスト
     * @return リクエストデータ
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map requestData(Http.Context ctx) {
        Object obj = new HashMap();
        if (ctx.request().body().asFormUrlEncoded() != null) {
            obj = ctx.request().body().asFormUrlEncoded();
        }

        Object obj1 = new HashMap();
        if (ctx.request().body().asMultipartFormData() != null) {
            obj1 = ctx.request().body().asMultipartFormData().asFormUrlEncoded();
        }

        Object obj2 = new HashMap();
        if (ctx.request().body().asJson() != null) {
            obj2 = Scala.asJava(FormUtils.fromJson("", play.api.libs.json.Json.parse(Json.stringify(ctx.request().body().asJson()))));
        }

        Map map = ctx.request().queryString();
        HashMap hashmap = new HashMap();
        Iterator iterator = ((Map) (obj)).keySet().iterator();
        do {
            if (!iterator.hasNext()) {
                break;
            }

            String s = (String) iterator.next();
            String as[] = (String[]) ((Map) (obj)).get(s);

            if (as.length == 1) {

                Pattern pattern = Pattern.compile("\\[[0-9]+\\]\\[([0-9]+)\\]");
                Matcher matcher = pattern.matcher(s);
                if (matcher.find()) {
                    int index = s.lastIndexOf("[");
                    int pos = Integer.parseInt(matcher.group(1));

                    String key = s.substring(0, index);

                    if(hashmap.containsKey(key)) {

                        String[] src = (String[])hashmap.get(key);

                        String[] dest = src;
                        if(src.length <= pos) {
                            // 足りないときは拡張する
                            dest = new String[pos + 1];

                            System.arraycopy(src, 0, dest, 0, src.length);
                        }

                        dest[pos] = as[0];

                        hashmap.put(key, dest);
                    } else {

                        String[] values = new String[pos + 1];
                        values[pos] = as[0];

                        hashmap.put(key, values);
                    }

                } else {
                    hashmap.put(s.replace("[]", ""), as[0]);
                }
            } else if(as.length > 1) {
                hashmap.put(s.replace("[]", ""), as);
            }
        } while (true);

        iterator = ((Map) (obj1)).keySet().iterator();
        do {
            if (!iterator.hasNext()) {
                break;
            }

            String s1 = (String) iterator.next();
            String as1[] = (String[]) ((Map) (obj1)).get(s1);
            if (as1.length == 1) {

                Pattern pattern = Pattern.compile("\\[[0-9]+\\]\\[[0-9]+\\]");
                Matcher matcher = pattern.matcher(s1);
                if (matcher.find()) {
                    int index = s1.lastIndexOf("[");
                    String key = s1.substring(0, index);

                    if(hashmap.containsKey(key)) {
                        String[] src = (String[])hashmap.get(key);
                        String[] dest = new String[src.length + 1];

                        System.arraycopy(src, 0, dest, 0, src.length);

                        dest[dest.length - 1] = as1[0];

                        hashmap.put(key, dest);
                    } else {
                        hashmap.put(key, new String[] { as1[0] });
                    }

                } else {
                    hashmap.put(s1.replace("[]", ""), as1[0]);
                }

            } else if(as1.length > 1) {
                hashmap.put(s1.replace("[]", ""), as1);
            }
        } while (true);
        String s2;
        for (iterator = ((Map) (obj2)).keySet().iterator(); iterator.hasNext(); hashmap.put(s2, ((Map) (obj2)).get(s2))) {
            s2 = (String) iterator.next();
        }
        iterator = map.keySet().iterator();
        do {
            if (!iterator.hasNext()) {
                break;
            }

            String s3 = (String) iterator.next();
            String as2[] = (String[]) map.get(s3);
            if (as2.length == 1) {
                Pattern pattern = Pattern.compile("\\[[0-9]+\\]\\[[0-9]+\\]");
                Matcher matcher = pattern.matcher(s3);
                if (matcher.find()) {
                    int index = s3.lastIndexOf("[");
                    String key = s3.substring(0, index);

                    if(hashmap.containsKey(key)) {
                        String[] src = (String[])hashmap.get(key);
                        String[] dest = new String[src.length + 1];

                        System.arraycopy(src, 0, dest, 0, src.length);

                        dest[dest.length - 1] = as2[0];

                        hashmap.put(key, dest);
                    } else {
                        hashmap.put(key, new String[] { as2[0] });
                    }

                } else {
                    hashmap.put(s3.replace("[]", ""), as2[0]);
                }
            } else if(as2.length > 1) {
                hashmap.put(s3.replace("[]", ""), as2);
            }
        } while (true);
        return hashmap;
    }

}

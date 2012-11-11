package direct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.TestModel;

import play.data.Form;
import play.mvc.Http.MultipartFormData.FilePart;

import direct.Direct.TYPE;
import static play.mvc.Controller.form;

@DirectClass
public class Test {

    @DirectFormParam
    public Map<String, String> params;

    @DirectUpload
    public List<FilePart> fileParts;

    @DirectMethod
    public Map<String, Object> execute(String value1, String value2) {

        Map<String, Object> result = new HashMap<String, Object>();

        result.put("success", true);

        return result;
    }

    @DirectMethod(type = TYPE.FORM_POST)
    public Map<String, Object> formPost() {

        Form<TestModel> form = form(TestModel.class).bind(params);
        System.out.println(form);

        Map<String, Object> result = new HashMap<String, Object>();

        result.put("success", true);

        return result;

    }

    @DirectMethod(type = TYPE.FORM_POST)
    public Map<String, Object> upload() {

        Form<TestModel> form = form(TestModel.class).bind(params);
        System.out.println(form);

        if(fileParts != null) {
            for (FilePart part : fileParts) {
                System.out.println(part.getFilename());
            }
        }

        Map<String, Object> result = new HashMap<String, Object>();

        result.put("success", true);

        return result;

    }

}

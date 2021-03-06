package direct;

import java.util.ArrayList;
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

    @DirectHttpParam
    public List<String> data;

    @DirectFormParam
    public Map<String, String> params;

    @DirectUpload
    public List<FilePart> fileParts;

    @DirectMethod(len = 2)
    public Map<String, Object> execute() {

        Map<String, Object> result = new HashMap<String, Object>();

        result.put("success", true);

        return result;
    }

    @DirectMethod(len = 3)
    public Map<String, Object> search() {

        System.out.println(data);

        Map<String, Object> result = new HashMap<String, Object>();

        List<Map<String, Object>> items = new ArrayList<Map<String,Object>>();
        Map<String, Object> item = null;

        item = new HashMap<String, Object>();
        item.put("name", "item A");
        items.add(item);

        item = new HashMap<String, Object>();
        item.put("name", "item B");
        items.add(item);

        item = new HashMap<String, Object>();
        item.put("name", "item C");
        items.add(item);

        result.put("success", true);
        result.put("items", items);

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

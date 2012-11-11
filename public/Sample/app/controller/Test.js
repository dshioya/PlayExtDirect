Ext.define('Sample.controller.Test', {

    extend: 'Ext.app.Controller',

    views: [
        'test.Panel'
    ],

    init: function() {
        var me = this;

        me.control({
            '.test_panel #push': {
                click: me.clickPushButton
            }
        });

    },

    clickPushButton: function() {

        Sample.direct.Test.execute(1, 2, function() {
            console.log(arguments);
        });

    }

});
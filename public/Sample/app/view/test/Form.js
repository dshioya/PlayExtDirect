Ext.define('Sample.view.test.Form', {

    extend: 'Ext.form.Panel',

    alias: 'widget.test_form',

    layout: 'form',

    border: false,

    bodyPadding: 10,

    defaults: {
        xtype: 'textfield'
    },

    frame: true,

    items: [
        {
            fieldLabel: 'メールアドレス',
            name: 'mail_address'
        },
        {
            fieldLabel: 'パスワード',
            inputType: 'password',
            name: 'password'
        },
        {
            fieldLabel: 'ファイル',
            xtype: 'filefield',
            name: 'imgFile'
        }
    ],

    constructor: function(config) {

        var me = this;

        config = config || {};

        me.initConfig(config);

        Ext.apply(me, {

            api: {
                submit: Sample.direct.Test.upload
            },

            paramOrder: ['mail_address', 'password'],

            dockedItems: [
                {
                    dock: 'bottom',

                    xtype: 'toolbar',

                    ui: 'footer',

                    items: [
                        {
                            text: '送信',
                            handler: me.onSubmit,
                            scope: me
                        }
                    ]
                }
            ]

        });

        me.callParent();

    },

    onSubmit: function() {
        var me = this;

        me.getForm().submit({
            params: {}
        });
    }

});
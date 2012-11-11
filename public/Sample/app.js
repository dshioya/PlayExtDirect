Ext.application({

    name: 'Sample',

    controllers: ['Test'],

    views: ['Viewport'],

    launch: function() {

        Ext.Direct.addProvider(Ext.app.REMOTING_API);

        Ext.create('Sample.view.Viewport');
    }
});
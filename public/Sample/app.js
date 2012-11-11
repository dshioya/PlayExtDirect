Ext.onReady(function() {
    Ext.Direct.addProvider(Ext.app.REMOTING_API);
});

Ext.application({

    name: 'Sample',

    controllers: ['Test'],

    stores: ['Tests'],

    views: ['Viewport'],

    launch: function() {

        Ext.create('Sample.view.Viewport');
    }
});
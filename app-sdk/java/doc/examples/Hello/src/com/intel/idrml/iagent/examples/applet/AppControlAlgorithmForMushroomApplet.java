package com.intel.idrml.iagent.examples.applet;

import com.intel.util.IntelApplet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.intel.idrml.iagent.framework.CoapException;
import com.intel.idrml.iagent.framework.IAgentManager;
import com.intel.idrml.iagent.framework.OnDataListener;
import com.intel.idrml.iagent.framework.OnRDEventListener;
import com.intel.idrml.iagent.model.DataQueryParam;
import com.intel.idrml.iagent.model.Device;
import com.intel.idrml.iagent.model.RDQueryParam;
import com.intel.idrml.iagent.model.Resource;
import com.intel.idrml.iagent.model.ResourceDataGeneral;
import com.intel.idrml.iagent.model.ResourceDataOCF;
import com.intel.idrml.iagent.utilities.LogUtil;

public class AppControlAlgorithmForMushroomApplet extends IntelApplet
{
  
	public int onInit(byte[] request)
	{
        AppControlAlgorithmForMushroom app = new AppControlAlgorithmForMushroom();
        app.start();
	    return APPLET_SUCCESS;
    }

	public int onClose()
	{

	    return APPLET_SUCCESS;
	}

	public int invokeCommand(int commandId, byte[] request)
	{
            return APPLET_SUCCESS;
    }

	
    class AppControlAlgorithmForMushroom implements OnDataListener {
		public static final String USER_ATTR_ROOM1 = "room1"; // this user attribute shall be configured on device "iagent", which value shall be set as group of related device
		public static final String USER_ATTR_ROOM2 = "room2"; // this user attribute shall be configured on device "iagent", which value shall be set as group of related device
		public static final String USER_ATTR_COOLER = "cooler"; // this user attribute shall be configured on "cooler" resource 
		public static final String USER_ATTR_HEATER = "heater"; // this user attribute shall be configured on "heater" resource
		public static final String USER_ATTR_LIGHTER = "lighter"; // this user attribute shall be configured on "lighter" resource
		public static final String USER_ATTR_FANS = "fans"; // this user attribute shall be configured on "cooler" resource
		public static final String USER_ATTR_SENSOR = "sensor"; // this user attribute shall be configured on those tempreture/humidity/lightness
		public static final String SENSOR_TYPE_TEMPER = "temperature";
		public static final String SENSOR_TYPE_HUMID = "humidity";
		public static final String SENSOR_TYPE_LIGHT = "lightness";
		private IAgentManager gateway;
	    private Map<String, MushRoom> rooms= new HashMap<String, MushRoom>();
	   	    
	    void start() {
	    	System.out.println("App started...");
	    	
	        gateway = IAgentManager.getInstance();
	     
	        // 0. get the room setting from user attribute of iagent
	        RDQueryParam query = new RDQueryParam();
	        query.standardType = "iagent"; // the default device in the gateway
	        List<Device> agents = gateway.DoDeviceQuery(query);
	        
	        if(agents==null || agents.size()==0){
	        	LogUtil.log("Warning: Error to get iagent device info!!!");
	        	return;
	        }
	        if(agents.get(0).getAttrs()==null){
	        	LogUtil.log("Warning: Error to get room info in iagent device!!!");
	        	return;
	        }
	        
	        // 1. monitor all devices that belong to group of "room" setted in attribute of iagent
	        String roomID1 = agents.get(0).getAttrs().get(USER_ATTR_ROOM1);
	        String roomID2 = agents.get(0).getAttrs().get(USER_ATTR_ROOM2);
	        rooms.put(roomID1, new MushRoom(roomID1));
	        rooms.put(roomID2, new MushRoom(roomID2));

	        RDQueryParam monitorQuery = new RDQueryParam();
	        monitorQuery.groupIds = new String[2];
	        monitorQuery.groupIds[0] = roomID1;
	        monitorQuery.groupIds[1] = roomID2;
	        try {
	            gateway.addRDMonitor(monitorQuery, new OnRDEventListener() {
	                public void onDeviceChanged(List<Device> devices) {
	                    LogUtil.log("OnDeviceEvent: " + devices.size() + " devices");
	                    // 2. monitor the data change for those resources with user attributes set as "sensor"
	                    monitorDataChangeForSensors(devices);
	                }
	                public void onResourceChanged(List<Resource> resourcesChanged, boolean isAddedOrRemove) 
	                { LogUtil.log("OnResourceEvent: " + resourcesChanged.size() + " resources"); }
	            });
	        } catch (CoapException e) {
	            e.printStackTrace();
	        }
	    }

		public void onResourceDataChanged(String deviceID, String resourceUri, ResourceDataGeneral resourceData) {
	        LogUtil.log("OnResourceData: " + deviceID + "/" + resourceUri + " value: \n" + resourceData);
	        
	        // 1. get the sensor type represented by the incoming resource data
	        Resource resource = gateway.getReource(deviceID, resourceUri);
	        
	        // 2. get the room id for the device id
	        Device device = gateway.getDevice(deviceID);
	        MushRoom room = rooms.get(device.getGroups().get(0));
	        
	        // 3. save the value to the room 
	        String sensorType = resource.getResourceType().get(0).replace("oic.r.", ""); //assume device is OCF device
	        room.setValue(sensorType, Integer.valueOf((((ResourceDataOCF)resourceData).getPropertyValue(sensorType).sv)));
	        
	        // 4. run algorithm for the room
	        room.runAlgorithm();
	        
	        // 5. report command to cloud
	    }
		
		
		
		

	    protected void monitorDataChangeForSensors(List<Device> devices) {
	    	for(Device device:devices)
	    	{
	    		String roomID = device.getGroups().get(0);
	    		rooms.get(roomID).updateSwitchControllers();

	    		for(Resource resource:device.getResources())
	    		{
	    			if(resource.getAttrs().contains(USER_ATTR_SENSOR))// assume user configure this sensor resource is to be monitored
	    			{
	    				DataQueryParam queryParam = new DataQueryParam(device.getDeviceId(), resource.getHref(), "", 10);
	    				try { 
	    					gateway.addDataMonitor(queryParam, this);
	    				} catch (CoapException e) { e.printStackTrace(); }
	    			}
	    		}
	    	}
		}

		private Resource findResourceByUserAttribute(String roomID, String userAttribute) {
			for(Device deviceItem: gateway.getAllDevices())
			{
				if(deviceItem.getGroups().contains(roomID))
				{
					for(Resource resource:deviceItem.getResources())
					{
						if(resource.getAttrs().contains(userAttribute))
						{
							return resource;
						}
					}
				}
			}
			return null;
		}
		
		private class MushRoom{
	    	public String id;
	    	public int temperature;
	    	public int humidity;
	    	public int lightness;
	    	public boolean lightSwitch;
	    	public boolean fanSwitch;
	    	public boolean coolerSwitch;
	    	public Map<String, Resource> switchsControllers;
	    	
	    	public MushRoom(String roomID) {
				id=roomID;
				lightSwitch = false;
				fanSwitch = false;
				coolerSwitch = false;
				switchsControllers = new HashMap<String, Resource>();
			}

	    	// This is a very simple example, which can be changed according to different scenario
			public void runAlgorithm()
	    	{
				if(temperature>40 && coolerSwitch == false){
					startCooler(); coolerSwitch = true;
				}
				else if(temperature<=40 && coolerSwitch == true){
					stopCooler(); coolerSwitch = false;
				}
				
				if(humidity>50 && fanSwitch == false){
					startFans(); fanSwitch = true;
				}
				else if(humidity<=50 && fanSwitch == true){
					stopFans(); fanSwitch = false;
				}
				
				if(lightness>50 && lightSwitch == false){
					turnOnLight(); lightSwitch=true;
				}
				else if(lightness<=50 && lightSwitch == true){
					turnOffLight(); lightSwitch=false;
				}
	    	}
	    	
			private void turnOnLight() {
				try {
					gateway.DoResourcePropertyPUT(switchsControllers.get(USER_ATTR_LIGHTER).getAbsoluteUri(), "1");
				} catch (CoapException e) {
					e.printStackTrace();
				}
			}

	    	private void turnOffLight() {
	    		try {
					gateway.DoResourcePropertyPUT(switchsControllers.get(USER_ATTR_LIGHTER).getAbsoluteUri(), "0");
				} catch (CoapException e) {
					e.printStackTrace();
				}
			}

			private void stopFans() {
				try {
					gateway.DoResourcePropertyPUT(switchsControllers.get(USER_ATTR_FANS).getAbsoluteUri(), "0");
				} catch (CoapException e) {
					e.printStackTrace();
				}
			}

			private void startFans() {
				try {
					gateway.DoResourcePropertyPUT(switchsControllers.get(USER_ATTR_FANS).getAbsoluteUri(), "0");
				} catch (CoapException e) {
					e.printStackTrace();
				}
			}

			private void stopCooler() {
				try {
					gateway.DoResourcePropertyPUT(switchsControllers.get(USER_ATTR_COOLER).getAbsoluteUri(), "0");
				} catch (CoapException e) {
					e.printStackTrace();
				}
			}

			private void startCooler() {
				try {
					gateway.DoResourcePropertyPUT(switchsControllers.get(USER_ATTR_COOLER).getAbsoluteUri(), "1");
				} catch (CoapException e) {
					e.printStackTrace();
				}
			}

			public void updateSwitchControllers() {
	    		switchsControllers.put(USER_ATTR_COOLER, findResourceByUserAttribute(id, USER_ATTR_COOLER));
	    		switchsControllers.put(USER_ATTR_LIGHTER, findResourceByUserAttribute(id, USER_ATTR_LIGHTER));
	    		switchsControllers.put(USER_ATTR_HEATER, findResourceByUserAttribute(id, USER_ATTR_HEATER));
	    		switchsControllers.put(USER_ATTR_FANS, findResourceByUserAttribute(id, USER_ATTR_FANS));
			}

			public void setValue(String sensorType, int value) {
				if(sensorType.equals(SENSOR_TYPE_TEMPER))
				{
					temperature = value; 
				}
				else if(sensorType.equals(SENSOR_TYPE_LIGHT))
				{
					lightness = value; 
				}
				else if(sensorType.equals(SENSOR_TYPE_HUMID))
				{
					humidity = value; 
				}
			};
	    }	
	}
}

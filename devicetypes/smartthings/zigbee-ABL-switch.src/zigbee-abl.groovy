metadata {
   definition (name: "ZigBee ITM-D-BZ Light", namespace: "smartthings", author: "SmartThings",runLocally: true, minHubCoreVersion: '000.022.00001', executeCommandsLocally: true, genericHandler: "Zigbee", mnmn : "Samsung Electronics", vid : "ABL-LIGHT-Z-001") {
       capability "Actuator"
       capability "Color Temperature"
       capability "Configuration"
       //capability "Refresh"
       capability "Switch"
       capability "Switch Level"
       capability "Health Check"
       capability "Light"
       capability "Bulb"
       fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0008,0300", outClusters : "0019", manufacturer: "Samsung Electronics", model: "ABL-LIGHT-Z-001", deviceJoinName: "Lithonia Lighting"
   }
   tiles(scale: 2) {
        multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#00A0DC", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#00A0DC", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
            }
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action:"switch level.setLevel"
            }
        }

        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        controlTile("colorTempSliderControl", "device.colorTemperature", "slider", width: 4, height: 2, inactiveLabel: false, range:"(2700..5000)") {
            state "colorTemperature", action:"color temperature.setColorTemperature"
        }

        main(["switch"])
        details(["switch", "colorTempSliderControl", "colorName", "refresh"])
    }
}

// Globals
private getMOVE_TO_COLOR_TEMPERATURE_COMMAND() { 0x0A }
private getCOLOR_CONTROL_CLUSTER() { 0x0300 }
private getATTRIBUTE_COLOR_TEMPERATURE() { 0x0007 }
// Parse incoming device messages to generate events
def parse(String description) {
   log.debug "parse: description is $description"
   def event = zigbee.getEvent(description)
   if (event) {
       if (event.name == "colorTemperature") {
           event.unit = "K"
       }
       log.debug "parse: Sending event $event"
       sendEvent(event)
   }
   else {
       log.warn "parse: DID NOT PARSE MESSAGE for description : $description"
       log.debug zigbee.parseDescriptionAsMap(description)
   }
}
def off() {
   zigbee.off()// + ["delay 1500"] + zigbee.onOffRefresh()
   //sendEvent(name: "switch", value: "off")
}
def on() {
   zigbee.on()// + ["delay 1500"] + zigbee.onOffRefresh()
   //sendEvent(name: "switch", value: "on")
}
def setLevel(value) {
   zigbee.setLevel(value) //+ ["delay 1500"] + zigbee.levelRefresh() + zigbee.onOffRefresh()
}
def refresh() {
   def cmds =  zigbee.levelRefresh() + zigbee.colorTemperatureRefresh() + zigbee.onOffRefresh()
   cmds
}

def ping() {
   return zigbee.levelRefresh()
}

def configure() {
   log.debug "configure()"
   sendEvent(name: "checkInterval", value: 12 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
   zigbee.onOffConfig() + zigbee.levelConfig() + zigbee.onOffRefresh() + zigbee.levelRefresh() + zigbee.colorTemperatureRefresh()
}
def updated() {
   log.debug "updated()"
   //configureHealthCheck()
}
def setColorTemperature(value) {
   value = value as Integer
   def tempInMired = Math.round(1000000 / value)
   def finalHex = zigbee.swapEndianHex(zigbee.convertToHexString(tempInMired, 4))
   zigbee.command(COLOR_CONTROL_CLUSTER, MOVE_TO_COLOR_TEMPERATURE_COMMAND, "$finalHex 0F00") +
   ["delay 3000"] +
   zigbee.readAttribute(COLOR_CONTROL_CLUSTER, ATTRIBUTE_COLOR_TEMPERATURE)
}
def installed() {
   configure()
}

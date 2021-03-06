/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.rsf.console.commands;
import net.hasor.core.Singleton;
import net.hasor.rsf.RsfBindInfo;
import net.hasor.rsf.RsfContext;
import net.hasor.rsf.console.RsfCommand;
import net.hasor.rsf.console.RsfCommandRequest;
import net.hasor.rsf.console.RsfInstruct;
import net.hasor.rsf.domain.RsfConstants;
import org.more.util.StringUtils;

import java.io.StringWriter;
import java.util.List;
/**
 *
 * @version : 2016年4月3日
 * @author 赵永春(zyc@hasor.net)
 */
@Singleton
@RsfCommand("list")
public class ListRsfInstruct implements RsfInstruct {
    //
    @Override
    public String helpInfo() {
        return "show service info.\r\n"//
                + " - list      (show all service id list.)\r\n"// 
                + " - list -h   (show help info.)\r\n"// 
                + " - list xxxx (show service info of XXXX.)";
    }
    @Override
    public boolean inputMultiLine(RsfCommandRequest request) {
        return false;
    }
    @Override
    public String doCommand(RsfCommandRequest request) throws Throwable {
        RsfContext rsfContext = request.getRsfContext();
        StringWriter sw = new StringWriter();
        String[] args = request.getRequestArgs();
        if (args != null && args.length > 0) {
            String todoArg = args[0];
            if (StringUtils.equalsIgnoreCase("-h", todoArg)) {
                sw.write(helpInfo());
            } else {
                String serviceID = todoArg;
                RsfBindInfo<?> info = rsfContext.getServiceInfo(serviceID);
                //
                sw.write(">>\r\n");
                sw.write(">>----- Service Info ------\r\n");
                sw.write(">>        ID :" + info.getBindID() + "\r\n");
                sw.write(">>     Group :" + info.getBindGroup() + "\r\n");
                sw.write(">>      Name :" + info.getBindName() + "\r\n");
                sw.write(">>   Version :" + info.getBindVersion() + "\r\n");
                sw.write(">>   Timeout :" + info.getClientTimeout() + "\r\n");
                sw.write(">> Serialize :" + info.getSerializeType() + "\r\n");
                sw.write(">>  javaType :" + info.getBindType().getName() + "\r\n");
                sw.write(">>\r\n");
                sw.write(">>----- Advanced Info -----\r\n");
                sw.write(">>   Message :" + info.isMessage() + "\r\n");
                sw.write(">>    Shadow :" + info.isShadow() + "\r\n");
                sw.write(">> SinglePool :" + info.isSharedThreadPool() + "\r\n");
                //
                sw.write(">>\r\n");
                sw.write(">>---- Subscribe Info -----\r\n");
                boolean isProvider = rsfContext.getServiceProvider(info) != null;
                sw.write(">>          Type :" + ((isProvider) ? "Provider" : "Consumer") + "\r\n");
                //
                sw.write(">>\r\n");
                sw.write(">>------ CenterInfo -------\r\n");
                sw.write(">>        Ticket :" + info.getMetaData(RsfConstants.Center_Ticket) + "\r\n");
                //
            }
            //
        } else {
            sw.write(">>>>>>>>>>>>>>>>>>>>>>>>  list  <<<<<<<<<<<<<<<<<<<<<<<<\r\n");
            List<String> serviceList = rsfContext.getServiceIDs();
            int maxLength = 0;
            for (String serviceID : serviceList) {
                maxLength = (maxLength < serviceID.length()) ? serviceID.length() : maxLength;
            }
            for (String serviceID : serviceList) {
                RsfBindInfo<?> info = rsfContext.getServiceInfo(serviceID);
                boolean isProvider = rsfContext.getServiceProvider(info) != null;
                String itemStr = StringUtils.rightPad(serviceID, maxLength) + "  -> " + ((isProvider) ? "Provider" : "Consumer");
                sw.write(">> " + itemStr + "\r\n");
            }
            //
        }
        return sw.toString();
    }
}
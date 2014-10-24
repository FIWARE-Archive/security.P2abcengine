//* Licensed Materials - Property of IBM, Miracle A/S, and            *
//* Alexandra Instituttet A/S                                         *
//* eu.abc4trust.pabce.1.0                                            *
//* (C) Copyright IBM Corp. 2012. All Rights Reserved.                *
//* (C) Copyright Miracle A/S, Denmark. 2012. All Rights Reserved.    *
//* (C) Copyright Alexandra Instituttet A/S, Denmark. 2012. All       *
//* Rights Reserved.                                                  *
//* US Government Users Restricted Rights - Use, duplication or       *
//* disclosure restricted by GSA ADP Schedule Contract with IBM Corp. *
//*                                                                   *
//* This file is licensed under the Apache License, Version 2.0 (the  *
//* "License"); you may not use this file except in compliance with   *
//* the License. You may obtain a copy of the License at:             *
//*   http://www.apache.org/licenses/LICENSE-2.0                      *
//* Unless required by applicable law or agreed to in writing,        *
//* software distributed under the License is distributed on an       *
//* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY            *
//* KIND, either express or implied.  See the License for the         *
//* specific language governing permissions and limitations           *
//* under the License.                                                *
//*/**/****************************************************************

//This code was copied from the Code/core-abce/service-helper tree

package ch.zhaw.ficore.p2abc.services.helpers.issuer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IssuanceHelper {
	private static Logger logger = LogManager.getLogger(IssuanceHelper.class.getName());
	private static IssuanceHelper instance = null;
	
	private IssuanceHelper() {
		logger = LogManager.getLogger(IssuanceHelper.class.getName());
	}
	
	public static synchronized IssuanceHelper initInstanceForService(
            CryptoEngine cryptoEngine) throws Exception {
		
		logger.entry();
		
        if (instance != null) {
            throw logger.throwing(new IllegalStateException(
                    "initInstance can only be called once!"));
        }

        instance = new IssuanceHelper(cryptoEngine,
                systemAndIssuerParamsPrefix, fileStoragePrefix, new String[0],
                modules);

        return logger.exit(instance);
    }
}

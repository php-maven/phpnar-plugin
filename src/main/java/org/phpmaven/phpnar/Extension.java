/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.phpmaven.phpnar;



/**
 * A single extension information for php
 * 
 * @author mepeisen
 */
public class Extension {
    
    /**
     * true to enable; false to disable (passed via --enable-*** or --disable-***)
     */
    private Boolean enable;
    
    /**
     * true to enable; false to disable (passed to configure via --with-*** or --without-***)
     */
    private Boolean with;
    
    /**
     * true for shared, false for static
     */
    private Boolean shared;
    
    /**
     * The name of the extension
     */
    private String name;
    
    /**
     * Constructor
     */
    public Extension() {
        // empty
    }

    public Boolean getEnable() {
        return enable;
    }

    public Boolean getWith() {
        return with;
    }

    public Boolean getShared() {
        return shared;
    }

    public String getName() {
        return name;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public void setWith(Boolean with) {
        this.with = with;
    }

    public void setShared(Boolean shared) {
        this.shared = shared;
    }

    public void setName(String name) {
        this.name = name;
    }

    
}

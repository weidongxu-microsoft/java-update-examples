/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.management.samples;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;

import java.io.ByteArrayOutputStream;

/**
 * Utility class to generate SSH keys.
 */
public final class SSHShell {

    /**
     * Generates an SSH pair.
     *
     * @param passPhrase the byte array content to be uploaded
     * @param comment the name of the file for which the content will be saved into
     * @return SSH public and private key
     * @throws Exception exception thrown
     */
    public static SshPublicPrivateKey generateSSHKeys(String passPhrase, String comment) throws Exception {
        JSch jsch = new JSch();
        KeyPair keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA);
        ByteArrayOutputStream privateKeyBuff = new ByteArrayOutputStream(2048);
        ByteArrayOutputStream publicKeyBuff = new ByteArrayOutputStream(2048);

        keyPair.writePublicKey(publicKeyBuff, (comment != null) ? comment : "SSHCerts");

        if (passPhrase == null  || passPhrase.isEmpty()) {
            keyPair.writePrivateKey(privateKeyBuff);
        } else {
            keyPair.writePrivateKey(privateKeyBuff, passPhrase.getBytes());
        }

        return new SshPublicPrivateKey(privateKeyBuff.toString(), publicKeyBuff.toString());
    }

    /**
     * Internal class to retain the generate SSH keys.
     */
    public static class SshPublicPrivateKey {
        private String sshPublicKey;
        private String sshPrivateKey;

        /**
         * Constructor.
         * @param sshPrivateKey SSH private key
         * @param sshPublicKey SSH public key
         */
        public SshPublicPrivateKey(String sshPrivateKey, String sshPublicKey) {
            this.sshPrivateKey = sshPrivateKey;
            this.sshPublicKey = sshPublicKey;
        }

        /**
         * Get SSH public key.
         * @return public key
         */
        public String getSshPublicKey() {
            return sshPublicKey;
        }

        /**
         * Get SSH private key.
         * @return private key
         */
        public String getSshPrivateKey() {
            return sshPrivateKey;
        }

        /**
         * Set SSH public key.
         * @param sshPublicKey public key
         */
        public void setSshPublicKey(String sshPublicKey) {
            this.sshPublicKey = sshPublicKey;
        }

        /**
         * Set SSH private key.
         * @param sshPrivateKey private key
         */
        public void setSshPrivateKey(String sshPrivateKey) {
            this.sshPrivateKey = sshPrivateKey;
        }
    }
}

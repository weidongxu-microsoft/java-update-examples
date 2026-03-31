package com.microsoft.azure.storage.advanced;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.SharedAccessAccountPermissions;
import com.microsoft.azure.storage.SharedAccessAccountPolicy;
import com.microsoft.azure.storage.SharedAccessAccountResourceType;
import com.microsoft.azure.storage.SharedAccessAccountService;
import com.microsoft.azure.storage.SharedAccessProtocols;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.SharedAccessBlobPermissions;
import com.microsoft.azure.storage.blob.SharedAccessBlobPolicy;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;

public class SasTokenGenerator {

    private final CloudStorageAccount storageAccount;

    public SasTokenGenerator(CloudStorageAccount storageAccount) {
        this.storageAccount = storageAccount;
    }

    public String generateAccountSas(Duration validity)
            throws InvalidKeyException, StorageException {
        SharedAccessAccountPolicy policy = new SharedAccessAccountPolicy();
        policy.setPermissions(EnumSet.of(
                SharedAccessAccountPermissions.READ,
                SharedAccessAccountPermissions.WRITE,
                SharedAccessAccountPermissions.LIST
        ));
        policy.setSharedAccessStartTime(new Date());
        policy.setSharedAccessExpiryTime(
                Date.from(Instant.now().plus(validity)));
        policy.setResourceTypes(EnumSet.of(
                SharedAccessAccountResourceType.CONTAINER,
                SharedAccessAccountResourceType.OBJECT
        ));
        policy.setServices(EnumSet.of(
                SharedAccessAccountService.BLOB,
                SharedAccessAccountService.QUEUE
        ));
        policy.setProtocols(SharedAccessProtocols.HTTPS_ONLY);

        return storageAccount.generateSharedAccessSignature(policy);
    }

    public String generateContainerSas(CloudBlobContainer container,
                                        Duration validity,
                                        EnumSet<SharedAccessBlobPermissions> permissions)
            throws StorageException, InvalidKeyException {
        SharedAccessBlobPolicy policy = new SharedAccessBlobPolicy();
        policy.setPermissions(permissions);
        policy.setSharedAccessStartTime(new Date());
        policy.setSharedAccessExpiryTime(
                Date.from(Instant.now().plus(validity)));

        return container.generateSharedAccessSignature(policy, null);
    }

    public String generateReadOnlyContainerSas(CloudBlobContainer container,
                                                Duration validity)
            throws StorageException, InvalidKeyException {
        return generateContainerSas(container, validity,
                EnumSet.of(SharedAccessBlobPermissions.READ,
                           SharedAccessBlobPermissions.LIST));
    }

    public String generateFullAccessAccountSas(Duration validity)
            throws InvalidKeyException, StorageException {
        SharedAccessAccountPolicy policy = new SharedAccessAccountPolicy();
        policy.setPermissions(EnumSet.of(
                SharedAccessAccountPermissions.READ,
                SharedAccessAccountPermissions.WRITE,
                SharedAccessAccountPermissions.DELETE,
                SharedAccessAccountPermissions.LIST,
                SharedAccessAccountPermissions.ADD,
                SharedAccessAccountPermissions.CREATE,
                SharedAccessAccountPermissions.UPDATE
        ));
        policy.setSharedAccessStartTime(new Date());
        policy.setSharedAccessExpiryTime(
                Date.from(Instant.now().plus(validity)));
        policy.setResourceTypes(EnumSet.of(
                SharedAccessAccountResourceType.SERVICE,
                SharedAccessAccountResourceType.CONTAINER,
                SharedAccessAccountResourceType.OBJECT
        ));
        policy.setServices(EnumSet.of(
                SharedAccessAccountService.BLOB,
                SharedAccessAccountService.QUEUE,
                SharedAccessAccountService.TABLE,
                SharedAccessAccountService.FILE
        ));
        policy.setProtocols(SharedAccessProtocols.HTTPS_ONLY);

        return storageAccount.generateSharedAccessSignature(policy);
    }

    public CloudStorageAccount getStorageAccount() {
        return storageAccount;
    }
}

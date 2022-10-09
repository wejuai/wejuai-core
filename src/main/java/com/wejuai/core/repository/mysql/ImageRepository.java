package com.wejuai.core.repository.mysql;

import com.wejuai.entity.mysql.Image;
import com.wejuai.entity.mysql.ImageUploadType;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author ZM.Wang
 */
public interface ImageRepository extends JpaRepository<Image, String> {

    boolean existsByTypeAndOssKey(ImageUploadType type, String ossKey);

}

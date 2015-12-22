package uk.lancs.sharc.smat.model;

import com.orm.SugarRecord;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import uk.lancs.sharc.smat.service.SharcLibrary;

/**
 * <p>This class is a model of the Media entity</p>
 *
 * Created by SHARC
 * Date: May 2015.
 */
public class MediaModel extends SugarRecord {
    private Long mid;
    private Long designerId;
    private Long experienceId;
    private String contentType;
    private String content;
    private String context;
    private String name;
    private String caption;
    private String entityType;
    private Long entityId;
    private int size;
    private boolean mainMedia;
    private boolean visible;
    private int mediaOrder;
    private int commentCount;

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_AUDIO = "audio";
    public static final String TYPE_VIDEO = "video";

    public MediaModel(){
    }
    public MediaModel(Long id, Long designerId, Long experienceId, String contentType, String content, String context, String name, String caption,
                      String entityType, Long entityID, int size, boolean mainMedia, boolean visible, int order, int commentCount) {
        this.mid = id;
        this.designerId = designerId;
        this.experienceId = experienceId;
        this.contentType = contentType;
        this.content = content;
        this.context = context;
        this.name = name;
        this.caption = caption;
        this.name = name;
        this.caption = caption;
        this.entityType = entityType;
        this.entityId = entityID;
        this.size = size;
        this.mainMedia = mainMedia;
        this.visible = visible;
        this.mediaOrder = order;
        this.commentCount = commentCount;
    }

    public String getHTMLPresentation()
    {
        return SharcLibrary.getHTMLCodeForMedia(this.getId().toString(), "media", this.getNoOfLike(), this.commentCount,this.getContentType(), this.getContent(), this.getCaption(),false);
    }
    public Long getId() {
        return mid;
    }

    public void setId(Long id) {
        this.mid = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public int getNoOfLike() {
        return 0;
    }

    public void setNoOfComment(String noOfComment) {
        //this.noOfComment = noOfComment;
    }

    public void setNoOfLike(String noOfLike) {
        //this.noOfLike = noOfLike;
    }

    public String getContentType(){
        return contentType;
    }
    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public Long getDesignerId() {
        return designerId;
    }

    public void setDesignerId(Long designerId) {
        this.designerId = designerId;
    }

    public Long getExperienceId() {
        return experienceId;
    }

    public void setExperienceId(Long experienceId) {
        this.experienceId = experienceId;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isMainMedia() {
        return mainMedia;
    }

    public void setMainMedia(boolean mainMedia) {
        this.mainMedia = mainMedia;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getOrder() {
        return mediaOrder;
    }

    public void setOrder(int order) {
        this.mediaOrder = order;
    }


}

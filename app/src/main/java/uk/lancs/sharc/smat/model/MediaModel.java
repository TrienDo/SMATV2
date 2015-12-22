package uk.lancs.sharc.smat.model;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import uk.lancs.sharc.smat.service.SharcLibrary;

/**
 * <p>This class is a model of the Media entity</p>
 *
 * Created by SHARC
 * Date: May 2015.
 */
public class MediaModel {
    private String id;
    private String name;
    private String desc;
    private String entityType;
    private String content;
    private String context;
    private String noOfLike;
    private String noOfComment;
    private String type;
    private String entityID;

    public MediaModel(String id, String name, String desc, String entityType, String content, String context, String noOfLike, String type, String entityID) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.entityType = entityType;
        this.content = content;
        this.context = context;
        this.noOfLike = noOfLike;
        this.type = type;
        this.entityID = entityID;
    }

    public String getHTMLPresentation()
    {
        return SharcLibrary.getHTMLCodeForMedia(this.getId(), "media", this.getNoOfLike(), this.getNoOfComment(),this.getType(), this.getContent(), this.getName(),false);
    }
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        try {
            return URLDecoder.decode(name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        try {
            return URLDecoder.decode(desc,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getContent() {
        if(type.equalsIgnoreCase("text"))
        {
            try {
                return URLDecoder.decode(content,"UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
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

    public String getNoOfLike() {
        return noOfLike;
    }

    public String getNoOfComment() {
        return noOfComment;
    }

    public void setNoOfComment(String noOfComment) {
        this.noOfComment = noOfComment;
    }

    public void setNoOfLike(String noOfLike) {
        this.noOfLike = noOfLike;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEntityID() {
        return entityID;
    }

    public void setEntityID(String entityID) {
        this.entityID = entityID;
    }
}

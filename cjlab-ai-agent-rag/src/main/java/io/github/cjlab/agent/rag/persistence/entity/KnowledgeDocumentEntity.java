package io.github.cjlab.agent.rag.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.cjlab.mybatisplus.datasource.core.BaseDO;

@TableName("cjlab_knowledge_document")
public class KnowledgeDocumentEntity extends BaseDO {

    @TableId
    private String id;
    private String title;
    private String content;
    private String metadataJson;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

}

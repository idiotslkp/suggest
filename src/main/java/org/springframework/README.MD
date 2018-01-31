E:\IdeaProjects\esproject\esboot\src\main\java\org\springframework\data\elasticsearch\core\ElasticsearchTemplate.java

注释掉<br/>
        // 高亮的这块先注释掉......
//		if (searchQuery.getHighlightFields() != null) {
//			for (HighlightBuilder.Field highlightField : searchQuery.getHighlightFields()) {
//				searchRequest.addHighlightedField(highlightField);
//			}
//		}


org.springframework.data.elasticsearch.core.facet.request.TermFacetRequest
这个类注释掉了最后的包含以及不包含,不过这个类已经被废弃掉了,应该用不到才对.


// 这里有bug......这个构造方法目前不能用,会报错,因为likeTexts不能为空,暂时用不到.后续出现问题再考虑怎么修改
		MoreLikeThisQueryBuilder moreLikeThisQueryBuilder = new MoreLikeThisQueryBuilder(null, items);		
这里有问题.		后续出现问题采用搜索的形式来进行修改.
package ca.uhn.fhir.jpa.dao.predicate;

import ca.uhn.fhir.jpa.dao.SearchBuilder;
import ca.uhn.fhir.jpa.model.entity.ResourceIndexedSearchParamCoords;
import ca.uhn.fhir.jpa.model.entity.ResourceTable;
import ca.uhn.fhir.jpa.util.CoordCalculator;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.dstu2.resource.Location;
import ca.uhn.fhir.rest.param.QuantityParam;
import ca.uhn.fhir.rest.param.SpecialParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.hibernate.search.spatial.impl.Point;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
@Scope("prototype")
public class PredicateBuilderCoords extends BasePredicateBuilder implements IPredicateBuilder {

	PredicateBuilderCoords(SearchBuilder theSearchBuilder) {
		super(theSearchBuilder);
	}

	private Predicate createPredicateCoords(IQueryParameterType theParam,
														 String theResourceName,
														 String theParamName,
														 CriteriaBuilder theBuilder,
														 From<?, ResourceIndexedSearchParamCoords> theFrom) {
		String latitudeValue;
		String longitudeValue;
		Double distanceKm = 0.0;

		if (theParam instanceof TokenParam) { // DSTU3
			TokenParam param = (TokenParam) theParam;
			String value = param.getValue();
			String[] parts = value.split(":");
			if (parts.length != 2) {
				throw new IllegalArgumentException("Invalid position format '" + value + "'.  Required format is 'latitude:longitude'");
			}
			latitudeValue = parts[0];
			longitudeValue = parts[1];
			if (isBlank(latitudeValue) || isBlank(longitudeValue)) {
				throw new IllegalArgumentException("Invalid position format '" + value + "'.  Both latitude and longitude must be provided.");
			}
			QuantityParam distanceParam = myParams.getNearDistanceParam();
			if (distanceParam != null) {
				distanceKm = distanceParam.getValue().doubleValue();
			}
		} else if (theParam instanceof SpecialParam) { // R4
			SpecialParam param = (SpecialParam) theParam;
			String value = param.getValue();
			String[] parts = value.split("\\|");
			if (parts.length < 2 || parts.length > 4) {
				throw new IllegalArgumentException("Invalid position format '" + value + "'.  Required format is 'latitude|longitude' or 'latitude|longitude|distance' or 'latitude|longitude|distance|units'");
			}
			latitudeValue = parts[0];
			longitudeValue = parts[1];
			if (isBlank(latitudeValue) || isBlank(longitudeValue)) {
				throw new IllegalArgumentException("Invalid position format '" + value + "'.  Both latitude and longitude must be provided.");
			}
			if (parts.length >= 3) {
				String distanceString = parts[2];
				if (!isBlank(distanceString)) {
					distanceKm = Double.valueOf(distanceString);
				}
			}
		} else {
			throw new IllegalArgumentException("Invalid position type: " + theParam.getClass());
		}

		Predicate latitudePredicate;
		Predicate longitudePredicate;
		if (distanceKm == 0.0) {
			latitudePredicate = theBuilder.equal(theFrom.get("myLatitude"), latitudeValue);
			longitudePredicate = theBuilder.equal(theFrom.get("myLongitude"), longitudeValue);
		} else if (distanceKm < 0.0) {
			throw new IllegalArgumentException("Invalid " + Location.SP_NEAR_DISTANCE + " parameter '" + distanceKm + "' must be >= 0.0");
		} else {
			double latitudeDegrees = Double.parseDouble(latitudeValue);
			double longitudeDegrees = Double.parseDouble(longitudeValue);

			Point northPoint = CoordCalculator.findTarget(latitudeDegrees, longitudeDegrees, 0.0, distanceKm);
			Point eastPoint = CoordCalculator.findTarget(latitudeDegrees, longitudeDegrees, 90.0, distanceKm);
			Point southPoint = CoordCalculator.findTarget(latitudeDegrees, longitudeDegrees, 180.0, distanceKm);
			Point westPoint = CoordCalculator.findTarget(latitudeDegrees, longitudeDegrees, 270.0, distanceKm);

			latitudePredicate = theBuilder.and(
				theBuilder.greaterThanOrEqualTo(theFrom.get("myLatitude"), southPoint.getLatitude()),
				theBuilder.lessThanOrEqualTo(theFrom.get("myLatitude"), northPoint.getLatitude())
			);
			longitudePredicate = theBuilder.and(
				theBuilder.greaterThanOrEqualTo(theFrom.get("myLongitude"), westPoint.getLongitude()),
				theBuilder.lessThanOrEqualTo(theFrom.get("myLongitude"), eastPoint.getLongitude())
			);
		}
		Predicate singleCode = theBuilder.and(latitudePredicate, longitudePredicate);
		return combineParamIndexPredicateWithParamNamePredicate(theResourceName, theParamName, theFrom, singleCode);
	}

	@Override
	public Predicate addPredicate(String theResourceName,
											String theParamName,
											List<? extends IQueryParameterType> theList,
											SearchFilterParser.CompareOperation theOperation) {
		Join<ResourceTable, ResourceIndexedSearchParamCoords> join = createJoin(SearchBuilderJoinEnum.COORDS, theParamName);

		if (theList.get(0).getMissing() != null) {
			addPredicateParamMissing(theResourceName, theParamName, theList.get(0).getMissing(), join);
			return null;
		}

		List<Predicate> codePredicates = new ArrayList<Predicate>();
		for (IQueryParameterType nextOr : theList) {

			Predicate singleCode = createPredicateCoords(nextOr,
				theResourceName,
				theParamName,
				myBuilder,
				join
			);
			codePredicates.add(singleCode);
		}

		Predicate retVal = myBuilder.or(toArray(codePredicates));
		myQueryRoot.addPredicate(retVal);
		return retVal;
	}
}
